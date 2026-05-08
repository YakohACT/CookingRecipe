package main.java.UI;

import main.java.SwingMain;
import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        add(header, BorderLayout.NORTH);

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
