package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeAIService;
import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientCategory;
import main.java.Recipe.IngredientNormalizer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登録済み食材一覧を JTable で閲覧・編集する画面パネル。
 * 行のカテゴリーをコンボで変更でき、↑/↓ボタンで並び順を入れ替えできる。
 * 「変更を保存」を押すと、現在のテーブル内容を IngredientMaster 経由で
 * database.csv に上書き保存する。
 */
public class IngredientMasterPanel extends JPanel {

    private static final int COL_NO   = 0;
    private static final int COL_CAT  = 1;
    private static final int COL_NAME = 2;

    private final SwingMain owner;
    private final DefaultTableModel model;
    private final JTable table;

    /**
     * 食材管理パネルを構築する。
     * @param owner 共有状態(IngredientMaster)にアクセスするためのフレーム参照
     */
    public IngredientMasterPanel(SwingMain owner) {
        this.owner = owner;
        setLayout(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // ===== ヘッダー =====
        JLabel titleLabel = new JLabel("食材管理");
        titleLabel.setFont(Theme.FONT_TITLE);

        JLabel hint = new JLabel(
                "<html><body style='width:520px'>"
                + "カテゴリー列のセルをクリックすると変更できます。"
                + "行を選択して↑/↓で並び順を変更できます。"
                + "「変更を保存」を押すと database.csv に書き戻します。"
                + "</body></html>");
        hint.setFont(Theme.FONT_MAIN);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        UIComponents.addLeftAligned(header, titleLabel);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        UIComponents.addLeftAligned(header, hint);

        // 右上: 表記ゆれをAIで統一するボタン
        JButton btnUnify = buildUnifyButton();
        btnUnify.addActionListener(e -> detectAndUnifyVariants());
        JPanel northEast = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        northEast.setOpaque(false);
        northEast.add(btnUnify);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.CENTER);
        north.add(northEast, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        // ===== テーブル =====
        String[] cols = {"#", "カテゴリー", "食材名"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COL_CAT;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == COL_NO)  return Integer.class;
                if (col == COL_CAT) return IngredientCategory.class;
                return String.class;
            }
        };
        for (Ingredient ing : owner.getIngredientMaster().getAllIngredients()) {
            model.addRow(new Object[]{model.getRowCount() + 1, ing.getCategory(), ing.getName()});
        }

        table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // カテゴリー列はコンボで編集できるようにする
        JComboBox<IngredientCategory> catEditor = new JComboBox<>(IngredientCategory.values());
        table.getColumnModel().getColumn(COL_CAT).setCellEditor(new DefaultCellEditor(catEditor));

        // 列幅
        table.getColumnModel().getColumn(COL_NO).setPreferredWidth(50);
        table.getColumnModel().getColumn(COL_NO).setMaxWidth(80);
        table.getColumnModel().getColumn(COL_CAT).setPreferredWidth(140);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(280);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== 操作ボタン =====
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        JButton btnUp = new JButton("↑ 上へ");
        btnUp.addActionListener(e -> moveSelected(-1));
        JButton btnDown = new JButton("↓ 下へ");
        btnDown.addActionListener(e -> moveSelected(1));
        toolbar.add(new JLabel("選択行を:"));
        toolbar.add(btnUp);
        toolbar.add(btnDown);

        JButton btnSave = UIComponents.createPrimaryButton("変更を保存");
        btnSave.addActionListener(e -> save());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(toolbar, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(btnSave);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * 選択された行をテーブル内で1行分上または下へ移動する(複数選択対応)。
     * 端にある場合は何もしない。番号列は自動で振り直す。
     * @param delta -1=上、+1=下
     */
    private void moveSelected(int delta) {
        // テーブル編集中のセルは確定させてから移動する
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        int[] sel = table.getSelectedRows();
        if (sel.length == 0) return;
        Arrays.sort(sel);

        if (delta < 0) {
            if (sel[0] == 0) return;
            for (int i : sel) swapRows(i, i - 1);
            table.clearSelection();
            for (int i : sel) table.addRowSelectionInterval(i - 1, i - 1);
        } else {
            if (sel[sel.length - 1] >= model.getRowCount() - 1) return;
            for (int idx = sel.length - 1; idx >= 0; idx--) {
                int i = sel[idx];
                swapRows(i, i + 1);
            }
            table.clearSelection();
            for (int i : sel) table.addRowSelectionInterval(i + 1, i + 1);
        }
        refreshRowNumbers();
        // 移動先が見えるようにスクロール
        int focus = table.getSelectedRow();
        if (focus >= 0) table.scrollRectToVisible(table.getCellRect(focus, 0, true));
    }

    /**
     * テーブル上の2行のカテゴリー列・食材名列を入れ替える。
     * @param a 行A
     * @param b 行B
     */
    private void swapRows(int a, int b) {
        Object catA  = model.getValueAt(a, COL_CAT);
        Object nameA = model.getValueAt(a, COL_NAME);
        model.setValueAt(model.getValueAt(b, COL_CAT),  a, COL_CAT);
        model.setValueAt(model.getValueAt(b, COL_NAME), a, COL_NAME);
        model.setValueAt(catA,  b, COL_CAT);
        model.setValueAt(nameA, b, COL_NAME);
    }

    /**
     * 行番号列(#)を 1〜N に振り直す。
     */
    private void refreshRowNumbers() {
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(i + 1, i, COL_NO);
        }
    }

    /**
     * 「表記ゆれをAIで統一」ボタンの見た目を構築する(AI機能であることを示す紫色)。
     * @return スタイル適用済みの JButton
     */
    private JButton buildUnifyButton() {
        JButton btn = new JButton("✨ 表記ゆれをAIで統一");
        btn.setBackground(Theme.COLOR_AI);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * AIに食材名の表記ゆれを検出させ、確認のうえマスタとレシピへ統一を適用する。
     * AIの呼び出しはモーダル進捗ダイアログ付きでバックグラウンド実行する。
     */
    private void detectAndUnifyVariants() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        // Ollama 以外は APIキー必須
        if (owner.getCurrentAiProvider() != RecipeAIService.Provider.OLLAMA
                && owner.getCurrentApiKey().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "先に「AI設定」メニューでプロバイダーとAPIキーを設定してください。",
                    "AI未設定", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> names = new ArrayList<>();
        for (Ingredient ing : owner.getIngredientMaster().getAllIngredients()) names.add(ing.getName());
        if (names.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "食材が2件未満のため、表記ゆれ検出をスキップしました。",
                    "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // モーダル進捗ダイアログ(不確定バー)
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "AIが表記ゆれを解析中…", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JLabel msg = new JLabel("食材名を解析しています。しばらくお待ちください…");
        msg.setFont(Theme.FONT_MAIN);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        content.add(msg, BorderLayout.NORTH);
        content.add(bar, BorderLayout.CENTER);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setSize(Math.max(440, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(this);

        SwingWorker<List<IngredientNormalizer.VariantGroup>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<IngredientNormalizer.VariantGroup> doInBackground() throws Exception {
                RecipeAIService svc = new RecipeAIService();
                svc.setConfig(owner.getCurrentAiProvider(),
                        owner.getCurrentApiKey(),
                        owner.getCurrentModelName());
                return svc.detectIngredientVariants(names);
            }

            @Override
            protected void done() {
                dialog.dispose();
                List<IngredientNormalizer.VariantGroup> groups;
                try {
                    groups = get();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(IngredientMasterPanel.this,
                            "AIの呼び出しに失敗しました。\n設定内容やコンソールのエラーを確認してください。",
                            "エラー", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                    return;
                }
                handleDetectedGroups(groups);
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }

    /**
     * AIが検出したグループを現在のマスタと突き合わせて確認ダイアログを出し、
     * ユーザーがOKしたら統一を適用してテーブルを再読込する。
     * @param groups AIが返した表記ゆれグループ
     */
    private void handleDetectedGroups(List<IngredientNormalizer.VariantGroup> groups) {
        // 現在マスタに実在する名前だけに絞り込み、表示用に整形
        Set<String> existing = new HashSet<>();
        for (Ingredient ing : owner.getIngredientMaster().getAllIngredients()) existing.add(ing.getName());

        List<IngredientNormalizer.VariantGroup> effective = new ArrayList<>();
        for (IngredientNormalizer.VariantGroup g : groups) {
            if (g.canonical == null || !existing.contains(g.canonical)) continue;
            List<String> vs = new ArrayList<>();
            for (String v : g.variants) {
                if (v != null && existing.contains(v) && !v.equals(g.canonical)) vs.add(v);
            }
            if (!vs.isEmpty()) effective.add(new IngredientNormalizer.VariantGroup(g.canonical, vs));
        }

        if (effective.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "表記ゆれは見つかりませんでした。",
                    "検出結果", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下の表記ゆれが見つかりました。統一しますか？\n");
        sb.append("(食材マスタと、その食材を使っている全レシピを書き換えます)\n\n");
        for (IngredientNormalizer.VariantGroup g : effective) {
            sb.append(String.join("・", g.variants)).append("  →  ").append(g.canonical).append("\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(Theme.FONT_MAIN);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(460, Math.min(360, 90 + effective.size() * 22)));

        int ok = JOptionPane.showConfirmDialog(this, scroll, "統一の確認", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        IngredientNormalizer.ApplyResult result = IngredientNormalizer.apply(
                effective, owner.getIngredientMaster(), owner.getAllRecipeList());

        reloadTable();
        JOptionPane.showMessageDialog(this,
                "統一が完了しました。\n"
                + "統合した別表記: " + result.mergedNames + "件\n"
                + "更新したレシピ: " + result.updatedRecipes + "件",
                "完了", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * テーブルの内容を現在の食材マスタから再読込する(統一適用後の反映用)。
     */
    private void reloadTable() {
        model.setRowCount(0);
        for (Ingredient ing : owner.getIngredientMaster().getAllIngredients()) {
            model.addRow(new Object[]{model.getRowCount() + 1, ing.getCategory(), ing.getName()});
        }
    }

    /**
     * 現在のテーブル内容を IngredientMaster.replaceAll に渡して database.csv に保存する。
     */
    private void save() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        List<Ingredient> ordered = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            IngredientCategory cat = (IngredientCategory) model.getValueAt(i, COL_CAT);
            String name = String.valueOf(model.getValueAt(i, COL_NAME));
            if (name == null || name.isEmpty()) continue;
            ordered.add(new Ingredient(name, cat));
        }
        owner.getIngredientMaster().replaceAll(ordered);
        JOptionPane.showMessageDialog(this,
                ordered.size() + " 件の食材を保存しました。",
                "保存完了", JOptionPane.INFORMATION_MESSAGE);
        owner.showWelcome();
    }
}
