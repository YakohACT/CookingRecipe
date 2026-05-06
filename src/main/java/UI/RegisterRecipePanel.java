package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeAIService;
import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientCategory;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * レシピ登録/編集画面パネル（AI自動提案機能付き）。
 * editTarget が null なら新規登録、非nullなら既存レシピの更新として動作する
 */
public class RegisterRecipePanel extends JPanel {

    private final SwingMain owner;
    private final Recipe editTarget;
    private final JTextField titleField = UIComponents.createStyledTextField();
    private final JTextField urlField = UIComponents.createStyledTextField();
    private final JList<RecipeCategory> categoryList = new JList<>(RecipeCategory.values());
    private final DefaultListModel<Ingredient> selectedListModel = new DefaultListModel<>();

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

        JButton btnAi = buildAiButton();
        form.add(buildHeader(btnAi));
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        JComboBox<IngredientCategory> catCombo = new JComboBox<>(IngredientCategory.values());
        catCombo.setFont(Theme.FONT_MAIN);
        catCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        DefaultListModel<Ingredient> masterListModel = new DefaultListModel<>();
        JList<Ingredient> masterIngList = new JList<>(masterListModel);

        JList<Ingredient> selectedIngList = new JList<>(selectedListModel);
        // Ctrl/Shift で複数選択 → 一度に解除できる
        selectedIngList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
            if (selected != null && !selectedListModel.contains(selected)) {
                selectedListModel.addElement(selected);
            }
        });

        JButton btnRemove = new JButton("選択した食材を解除");
        btnRemove.addActionListener(e -> {
            int[] indices = selectedIngList.getSelectedIndices();
            // 末尾から削除すると先のインデックスが崩れない
            for (int i = indices.length - 1; i >= 0; i--) {
                selectedListModel.remove(indices[i]);
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
        UIComponents.addLeftAligned(form, new JLabel("使用する食材 (Ctrl+クリックで複数選択):"));
        JScrollPane sScroll = new JScrollPane(selectedIngList);
        sScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sScroll);
        UIComponents.addLeftAligned(form, btnRemove);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        UIComponents.addLeftAligned(form, btnSubmit);

        // 編集モードのときは既存レシピの内容をフォームに復元
        if (editTarget != null) {
            prefillFromRecipe(editTarget);
        }

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    /**
     * 編集対象レシピの内容をフォーム要素に流し込む。
     * @param r 編集対象のレシピ
     */
    private void prefillFromRecipe(Recipe r) {
        titleField.setText(r.getTitle());
        urlField.setText(r.getUrl());
        for (Ingredient ing : r.getIngredients()) {
            if (!selectedListModel.contains(ing)) selectedListModel.addElement(ing);
        }
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
        return headerPanel;
    }

    /**
     * AI自動提案ボタンの見た目を構築する。
     * @return スタイル適用済みの JButton
     */
    private JButton buildAiButton() {
        JButton btnAi = new JButton("✨ AI自動提案");
        btnAi.setBackground(Theme.COLOR_AI);
        btnAi.setForeground(Color.WHITE);
        btnAi.setOpaque(true);
        btnAi.setBorderPainted(false);
        btnAi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btnAi;
    }

    /**
     * AI自動提案を非同期で要求し、成功時はフォームに反映する。
     * @param btnAi 押下されたAIボタン(処理中は無効化される)
     */
    private void requestAiSuggestion(JButton btnAi) {
        // Ollama はローカルLLMなのでAPIキー不要。それ以外はAPIキー必須
        if (owner.getCurrentAiProvider() != RecipeAIService.Provider.OLLAMA
                && owner.getCurrentApiKey().isEmpty()) {
            JOptionPane.showMessageDialog(this, "先に「AI設定」メニューからAPIキーを入力してください");
            return;
        }
        btnAi.setText("生成中...");
        btnAi.setEnabled(false);

        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() throws Exception {
                RecipeAIService aiService = new RecipeAIService();
                aiService.setConfig(owner.getCurrentAiProvider(), owner.getCurrentApiKey(), owner.getCurrentModelName());
                return aiService.suggestRecipe(urlField.getText().trim(), owner.getIngredientMaster().getAllIngredients());
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
     * AIの返したレシピをフォームに反映する。
     * タイトルが空、または食材マスタとマッチする食材が0個の場合は
     * 「検出できなかった」と判断し、フォームには一切手を加えず false を返す。
     * @param result AIから返ってきた {タイトル, "食材1,食材2,…"} 配列
     * @return 反映に成功すれば true、検出失敗時は false
     */
    private boolean applyAiResult(String[] result) {
        String aiTitle = (result == null || result.length < 1 || result[0] == null) ? "" : result[0].trim();
        String aiIngsRaw = (result == null || result.length < 2 || result[1] == null) ? "" : result[1];

        ArrayList<Ingredient> matched = new ArrayList<>();
        for (String aiIngName : aiIngsRaw.split(",")) {
            String cleanName = aiIngName.trim();
            if (cleanName.isEmpty()) continue;
            for (Ingredient ing : owner.getIngredientMaster().getAllIngredients()) {
                if (ing.getName().equals(cleanName) && !matched.contains(ing)) {
                    matched.add(ing);
                }
            }
        }

        if (aiTitle.isEmpty() || matched.isEmpty()) {
            return false;
        }

        titleField.setText(aiTitle);
        selectedListModel.clear();
        for (Ingredient ing : matched) {
            selectedListModel.addElement(ing);
        }
        return true;
    }

    /**
     * 入力内容を検証し、新規登録または更新を行う。
     * 終了後はウェルカム画面に戻る。
     */
    private void submitRecipe() {
        String title = titleField.getText().trim();
        String url = urlField.getText().trim();

        if (title.isEmpty() || url.isEmpty() || selectedListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "入力されていない箇所があります", "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<Ingredient> ings = new ArrayList<>();
        for (int i = 0; i < selectedListModel.size(); i++) ings.add(selectedListModel.getElementAt(i));

        EnumSet<RecipeCategory> categories = EnumSet.noneOf(RecipeCategory.class);
        for (RecipeCategory cat : categoryList.getSelectedValuesList()) {
            categories.add(cat);
        }
        if (categories.isEmpty()) categories.add(RecipeCategory.OTHER);

        if (editTarget == null) {
            owner.getAllRecipeList().addRecipe(new Recipe(title, url, ings, categories));
            JOptionPane.showMessageDialog(this, "レシピ「" + title + "」を登録しました");
        } else {
            Recipe updated = new Recipe(title, url, ings, categories);
            updated.setId(editTarget.getId());
            owner.getAllRecipeList().updateRecipe(updated);
            JOptionPane.showMessageDialog(this, "レシピ「" + title + "」を更新しました");
        }
        owner.showWelcome();
    }
}
