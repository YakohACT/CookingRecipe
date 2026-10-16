package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeSuggestion;
import main.java.Recipe.BatchUrlImporter;
import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientCategory;
import main.java.Recipe.Quantity;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * レシピ登録/編集画面パネル（AI自動提案機能付き）。
 * editTarget が null なら新規登録、非nullなら既存レシピの更新として動作する
 * 使用する食材は表で管理し、食材ごとに数量と単位(分量)を入力できる
 */
public class RegisterRecipePanel extends JPanel {

    private static final int COL_INGREDIENT = 0;
    private static final int COL_AMOUNT = 1;
    private static final int COL_UNIT = 2;

    private final SwingMain owner;
    private final Recipe editTarget;
    private final JTextField titleField = UIComponents.createStyledTextField();
    private final JTextField urlField = UIComponents.createStyledTextField();
    private final JList<RecipeCategory> categoryList = new JList<>(RecipeCategory.values());
    /** 使用する食材の表(食材 / 数量 / 単位) */
    private final DefaultTableModel selectedModel;
    private final JTable selectedTable;

    /**
     * 新規登録モードで構築する。
     * @param owner 共有状態にアクセスするためのフレーム参照
     */
    public RegisterRecipePanel(SwingMain owner) {
        this(owner, null);
    }

    /**
     * 編集モード(または editTarget=null で新規登録モード)で構築する。
     * editTarget が非nullなら、フォームに既存値が prefill され保存ボタンが「更新」になる。
     * @param owner      共有状態にアクセスするためのフレーム参照
     * @param editTarget 編集対象レシピ。null なら新規登録モード
     */
    public RegisterRecipePanel(SwingMain owner, Recipe editTarget) {
        this.owner = owner;
        this.editTarget = editTarget;
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        JButton btnAi = UIComponents.createAiButton("✨ AI自動提案");
        form.add(buildHeader(btnAi));
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        JComboBox<IngredientCategory> catCombo = new JComboBox<>(IngredientCategory.values());
        catCombo.setFont(Theme.FONT_MAIN);
        catCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        DefaultListModel<Ingredient> masterListModel = new DefaultListModel<>();
        JList<Ingredient> masterIngList = new JList<>(masterListModel);

        selectedModel = new DefaultTableModel(new String[]{"食材", "数量", "単位"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != COL_INGREDIENT;
            }
        };
        selectedTable = buildSelectedTable();

        catCombo.addActionListener(e -> {
            IngredientCategory cat = (IngredientCategory) catCombo.getSelectedItem();
            masterListModel.clear();
            if (cat != null) {
                for (Ingredient ing : owner.getIngredientMaster().searchIngredient(cat)) {
                    masterListModel.addElement(ing);
                }
            }
        });

        JButton btnAdd = new JButton("選択した食材を追加");
        btnAdd.addActionListener(e -> {
            Ingredient selected = masterIngList.getSelectedValue();
            if (selected != null) addIngredientRow(selected, Quantity.EMPTY);
        });

        JButton btnRemove = new JButton("選択した食材を解除");
        btnRemove.addActionListener(e -> {
            stopEditing();
            int[] rows = selectedTable.getSelectedRows();
            // 末尾から削除すると先のインデックスが崩れない
            for (int i = rows.length - 1; i >= 0; i--) {
                selectedModel.removeRow(rows[i]);
            }
        });

        btnAi.addActionListener(e -> requestAiSuggestion(btnAi));

        JButton btnSubmit = UIComponents.createPrimaryButton(editTarget == null ? "レシピを保存" : "レシピを更新");
        btnSubmit.addActionListener(e -> submitRecipe());

        categoryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        categoryList.setVisibleRowCount(6);
        categoryList.setFont(Theme.FONT_MAIN);
        JScrollPane categoryScroll = new JScrollPane(categoryList);
        categoryScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        categoryScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        UIComponents.addLeftAligned(form, new JLabel("タイトル:"));
        UIComponents.addLeftAligned(form, titleField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        UIComponents.addLeftAligned(form, new JLabel("URL:"));
        UIComponents.addLeftAligned(form, urlField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        UIComponents.addLeftAligned(form, new JLabel("レシピのカテゴリー (Ctrl+クリックで複数選択可):"));
        form.add(categoryScroll);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        UIComponents.addLeftAligned(form, new JLabel("カテゴリから食材を選択:"));
        UIComponents.addLeftAligned(form, catCombo);

        JScrollPane mScroll = new JScrollPane(masterIngList);
        mScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(mScroll);
        UIComponents.addLeftAligned(form, btnAdd);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        UIComponents.addLeftAligned(form, new JLabel(
                "使用する食材と分量 (数量・単位のセルをクリックして入力。単位は候補から選ぶか直接入力):"));
        JScrollPane sScroll = new JScrollPane(selectedTable);
        sScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sScroll);
        UIComponents.addLeftAligned(form, btnRemove);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        UIComponents.addLeftAligned(form, btnSubmit);

        // 編集モードのときは既存レシピの内容をフォームに復元
        if (editTarget != null) {
            prefillFromRecipe(editTarget);
        }

        add(UIComponents.createScrollPane(form), BorderLayout.CENTER);
    }

    /**
     * 使用する食材の表を構築する。
     * 単位列は候補から選べる編集可能なコンボにし、Ctrl/Shift で複数行を選択して一度に解除できる
     * @return 構築した JTable
     */
    private JTable buildSelectedTable() {
        JTable table = new JTable(selectedModel);
        table.setRowHeight(24);
        table.setFont(Theme.FONT_MAIN);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        // 入力途中で別の部品をクリックしても、入力中の値を確定させる
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setPreferredScrollableViewportSize(new Dimension(450, 200));

        JComboBox<String> unitEditor = new JComboBox<>(Quantity.COMMON_UNITS.toArray(new String[0]));
        unitEditor.setEditable(true);
        table.getColumnModel().getColumn(COL_UNIT).setCellEditor(new DefaultCellEditor(unitEditor));

        table.getColumnModel().getColumn(COL_INGREDIENT).setPreferredWidth(240);
        table.getColumnModel().getColumn(COL_AMOUNT).setPreferredWidth(90);
        table.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(120);
        return table;
    }

    /**
     * 使用する食材の表に1行追加する。既に同じ食材がある場合は何もしない。
     * @param ing      追加する食材
     * @param quantity その食材の分量
     */
    private void addIngredientRow(Ingredient ing, Quantity quantity) {
        for (int i = 0; i < selectedModel.getRowCount(); i++) {
            if (ing.equals(selectedModel.getValueAt(i, COL_INGREDIENT))) return;
        }
        selectedModel.addRow(new Object[]{ing, quantity.getAmount(), quantity.getUnit()});
    }

    /**
     * 表で入力中のセルがあれば値を確定させる。
     */
    private void stopEditing() {
        if (selectedTable.isEditing()) selectedTable.getCellEditor().stopCellEditing();
    }

    /**
     * 使用する食材の表を指定レシピの食材と分量で置き換える。
     * @param r 表示するレシピ
     */
    private void showIngredients(Recipe r) {
        stopEditing();
        selectedModel.setRowCount(0);
        for (Ingredient ing : r.getIngredients()) {
            addIngredientRow(ing, r.getQuantity(ing));
        }
    }

    /**
     * 編集対象レシピの内容をフォーム要素に流し込む。
     * @param r 編集対象のレシピ
     */
    private void prefillFromRecipe(Recipe r) {
        titleField.setText(r.getTitle());
        urlField.setText(r.getUrl());
        showIngredients(r);
        // カテゴリーJListの選択インデックスを同期
        RecipeCategory[] all = RecipeCategory.values();
        EnumSet<RecipeCategory> cats = r.getCategories();
        int[] indices = new int[cats.size()];
        int idx = 0;
        for (int i = 0; i < all.length; i++) {
            if (cats.contains(all[i])) indices[idx++] = i;
        }
        categoryList.setSelectedIndices(indices);
    }

    /**
     * フォームヘッダー(タイトル文字列 + AIボタン)を構築する。
     * @param btnAi AI自動提案ボタン
     * @return ヘッダーパネル
     */
    private JPanel buildHeader(JButton btnAi) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(editTarget == null ? "新規レシピ登録" : "レシピ編集");
        titleLabel.setFont(Theme.FONT_TITLE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(btnAi, BorderLayout.EAST);
        // ウィンドウを広げたときにヘッダーが縦に伸びて AIボタンが巨大化しないよう高さを固定する
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));
        return headerPanel;
    }

    /**
     * AI自動提案を非同期で要求し、成功時はフォームに反映する。
     * @param btnAi 押下されたAIボタン(処理中は無効化される)
     */
    private void requestAiSuggestion(JButton btnAi) {
        if (!UIComponents.ensureAiReady(this, owner.getAiSettings())) return;
        btnAi.setText("生成中...");
        btnAi.setEnabled(false);

        SwingWorker<RecipeSuggestion, Void> worker = new SwingWorker<>() {
            @Override
            protected RecipeSuggestion doInBackground() throws Exception {
                return owner.getAiSettings().createService().suggestRecipe(
                        urlField.getText().trim(), owner.getIngredientMaster().getAllIngredientNames());
            }

            @Override
            protected void done() {
                try {
                    if (applyAiResult(get())) {
                        JOptionPane.showMessageDialog(RegisterRecipePanel.this, "AIがレシピを提案しました");
                    }
                    // 検出できなかった場合はフォームを書き換えず、ダイアログも出さない
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RegisterRecipePanel.this,
                            "AIの呼び出しに失敗しました。\n設定内容やコンソールのエラーを確認してください。",
                            "エラー", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } finally {
                    btnAi.setText("✨ AI自動提案");
                    btnAi.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /**
     * AIの返したレシピ(タイトル・食材・分量)をフォームに反映する。
     * タイトルが空、または食材が0個の場合は「検出できなかった」と判断し、
     * フォームには一切手を加えず false を返す。カテゴリーの選択は変更しない。
     * @param suggestion AIが提案したレシピ
     * @return 反映に成功すれば true、検出失敗時は false
     */
    private boolean applyAiResult(RecipeSuggestion suggestion) {
        // AIが未知の食材を返した場合は OTHER カテゴリで自動的にマスタに追加される
        Recipe suggested = BatchUrlImporter.toRecipe(suggestion, urlField.getText().trim(), owner.getIngredientMaster());
        if (suggested == null) return false;

        titleField.setText(suggested.getTitle());
        showIngredients(suggested);
        return true;
    }

    /**
     * 入力内容を検証し、新規登録または更新を行う。
     * 終了後はウェルカム画面に戻る。
     */
    private void submitRecipe() {
        stopEditing();
        String title = titleField.getText().trim();
        String url = urlField.getText().trim();

        if (title.isEmpty() || url.isEmpty() || selectedModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "入力されていない箇所があります", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<Ingredient> ings = new ArrayList<>();
        Map<String, Quantity> quantities = new LinkedHashMap<>();
        for (int i = 0; i < selectedModel.getRowCount(); i++) {
            Ingredient ing = (Ingredient) selectedModel.getValueAt(i, COL_INGREDIENT);
            ings.add(ing);
            quantities.put(ing.getName(), Quantity.of(
                    (String) selectedModel.getValueAt(i, COL_AMOUNT), (String) selectedModel.getValueAt(i, COL_UNIT)));
        }

        // 未選択の場合は Recipe 側で OTHER になる
        EnumSet<RecipeCategory> categories = EnumSet.noneOf(RecipeCategory.class);
        for (RecipeCategory cat : categoryList.getSelectedValuesList()) {
            categories.add(cat);
        }

        Recipe recipe = new Recipe(title, url, ings, quantities, categories);
        if (editTarget == null) {
            owner.getAllRecipeList().addRecipe(recipe);
            JOptionPane.showMessageDialog(this, "レシピ「" + title + "」を登録しました");
        } else {
            recipe.setId(editTarget.getId());
            owner.getAllRecipeList().updateRecipe(recipe);
            JOptionPane.showMessageDialog(this, "レシピ「" + title + "」を更新しました");
        }
        owner.showWelcome();
    }
}
