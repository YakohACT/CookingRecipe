package main.java.UI;

import main.java.SwingMain;
import main.java.Recipe.Recipe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * レシピ削除画面パネル
 */
public class DeleteRecipePanel extends JPanel {

    /**
     * 削除パネルを構築する。
     * @param owner 共有状態(レシピリスト等)へアクセスするためのフレーム参照
     */
    public DeleteRecipePanel(SwingMain owner) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(30, 50, 30, 50));

        DefaultListModel<Recipe> listModel = new DefaultListModel<>();
        for (Recipe r : owner.getAllRecipeList().getRecipeList()) listModel.addElement(r);
        JList<Recipe> list = UIComponents.createStyledRecipeList(listModel);
        list.setPreferredSize(new Dimension(0, 0));

        JButton btnDel = new JButton("選択したレシピを削除");
        btnDel.setBackground(Theme.COLOR_DELETE);
        btnDel.setForeground(Color.WHITE);
        btnDel.setOpaque(true);
        btnDel.setBorderPainted(false);
        btnDel.addActionListener(e -> {
            Recipe selected = list.getSelectedValue();
            if (selected == null) return;
            int result = JOptionPane.showConfirmDialog(this,
                    "本当に「" + selected.getTitle() + "」を削除しますか？",
                    "削除の確認", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                owner.getAllRecipeList().deleteRecipe(selected);
                listModel.removeElement(selected);
            }
        });

        JLabel label = new JLabel("削除するレシピを選択してください");
        label.setFont(Theme.FONT_MAIN);
        add(label, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(btnDel, BorderLayout.SOUTH);
    }
}
