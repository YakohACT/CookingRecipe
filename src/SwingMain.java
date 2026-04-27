import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * レシピ管理システムのGUIメインクラス
 */
public class SwingMain extends JFrame {
    private AllRecipeList allRecipeList;
    private IngredientMaster ingredientMaster;
    private JPanel centerPanel;

    private final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private final Color COLOR_SIDE = new Color(44, 62, 80);
    private final Font FONT_MAIN = new Font("SansSerif", Font.PLAIN, 14);
    private final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);

    public SwingMain() {
        allRecipeList = new AllRecipeList();
        ingredientMaster = new IngredientMaster();

        setTitle("Recipe Manager Pro");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                allRecipeList.write();
                System.exit(0);
            }
        });

        JPanel sideMenu = new JPanel();
        sideMenu.setLayout(new BoxLayout(sideMenu, BoxLayout.Y_AXIS));
        sideMenu.setBackground(COLOR_SIDE);
        sideMenu.setPreferredSize(new Dimension(200, 0));
        sideMenu.setBorder(new EmptyBorder(30, 10, 10, 10));

        addSideButton(sideMenu, "レシピ登録", e -> showRegisterForm());
        addSideButton(sideMenu, "レシピ閲覧", e -> showViewMenu());
        addSideButton(sideMenu, "レシピ削除", e -> showDeleteForm());
        sideMenu.add(Box.createVerticalGlue());
        addSideButton(sideMenu, "保存して終了", e -> { allRecipeList.write(); System.exit(0); });

        add(sideMenu, BorderLayout.WEST);
        centerPanel = new JPanel(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);
        showWelcomeMessage();
    }

    private void addSideButton(JPanel p, String t, java.awt.event.ActionListener a) {
        JButton b = new JButton(t);
        b.setMaximumSize(new Dimension(180, 40));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(COLOR_SIDE);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.addActionListener(a);
        p.add(b);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void showWelcomeMessage() {
        centerPanel.removeAll();
        JLabel l = new JLabel("<html><center>Welcome!<br>操作を選択してください</center></html>", SwingConstants.CENTER);
        l.setFont(FONT_TITLE);
        centerPanel.add(l, BorderLayout.CENTER);
        updatePanel();
    }

    private void showRegisterForm() {
        centerPanel.removeAll();
        JPanel f = new JPanel(); f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBorder(new EmptyBorder(30, 50, 30, 50));
        f.setBackground(Color.WHITE);

        JPanel h = new JPanel(new BorderLayout()); h.setBackground(Color.WHITE);
        JLabel l = new JLabel("新規レシピ登録"); l.setFont(FONT_TITLE); h.add(l, BorderLayout.WEST);
        JButton ai = new JButton("✨ AI自動提案"); ai.setBackground(new Color(155, 89, 182)); ai.setForeground(Color.WHITE);
        ai.setOpaque(true); ai.setBorderPainted(false); h.add(ai, BorderLayout.EAST);
        addLeftAligned(f, h);

        JTextField tf = createField(); JTextField uf = createField();
        DefaultListModel<Ingredient> sm = new DefaultListModel<>(); JList<Ingredient> sl = new JList<>(sm);
        DefaultListModel<Ingredient> mm = new DefaultListModel<>(); JList<Ingredient> ml = new JList<>(mm);
        JComboBox<IngredientCategory> cb = new JComboBox<>(IngredientCategory.values());
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cb.addActionListener(e -> {
            mm.clear();
            for(Ingredient i : ingredientMaster.searchIngredient((IngredientCategory)cb.getSelectedItem())) mm.addElement(i);
        });

        ai.addActionListener(e -> {
            ai.setText("生成中..."); ai.setEnabled(false);
            new SwingWorker<String[], Void>() {
                protected String[] doInBackground() throws Exception { return new RecipeAIService().suggestRecipe(ingredientMaster.getAllIngredients()); }
                protected void done() {
                    try {
                        String[] r = get(); tf.setText(r[0]); sm.clear();
                        for(String n : r[1].split(",")) {
                            for(Ingredient i : ingredientMaster.getAllIngredients()) if(i.getName().equals(n.trim())) sm.addElement(i);
                        }
                    } catch(Exception ex) { JOptionPane.showMessageDialog(null, "AI連携失敗"); }
                    ai.setText("✨ AI自動提案"); ai.setEnabled(true);
                }
            }.execute();
        });

        JButton sub = new JButton("レシピを保存"); sub.setBackground(COLOR_PRIMARY); sub.setForeground(Color.WHITE);
        sub.setOpaque(true); sub.setBorderPainted(false);
        sub.addActionListener(e -> {
            if(tf.getText().isEmpty() || sm.isEmpty()) { JOptionPane.showMessageDialog(null, "入力されていない箇所があります"); return; }
            ArrayList<Ingredient> ings = new ArrayList<>();
            for(int i=0; i<sm.size(); i++) ings.add(sm.getElementAt(i));
            allRecipeList.addRecipe(new Recipe(tf.getText(), uf.getText(), ings));
            showWelcomeMessage();
        });

        addLeftAligned(f, new JLabel("タイトル:")); addLeftAligned(f, tf);
        addLeftAligned(f, new JLabel("URL:")); addLeftAligned(f, uf);
        addLeftAligned(f, new JLabel("食材選択:")); addLeftAligned(f, cb);
        f.add(new JScrollPane(ml));
        JButton addBtn = new JButton("追加"); addBtn.addActionListener(e -> { if(ml.getSelectedValue()!=null) sm.addElement(ml.getSelectedValue()); });
        addLeftAligned(f, addBtn);
        addLeftAligned(f, new JLabel("追加済み食材:")); f.add(new JScrollPane(sl));
        addLeftAligned(f, sub);

        centerPanel.add(new JScrollPane(f));
        updatePanel();
    }

    private void showViewMenu() {
        centerPanel.removeAll();
        JTabbedPane t = new JTabbedPane();
        JPanel p1 = new JPanel(new BorderLayout(10,10));
        DefaultListModel<Recipe> m = new DefaultListModel<>();
        for(Recipe r : allRecipeList.getRecipeList()) m.addElement(r);
        JList<Recipe> l = createList(m); JTextArea d = createArea();
        l.addListSelectionListener(e -> { if(l.getSelectedValue()!=null) updateArea(d, l.getSelectedValue()); });
        p1.add(new JScrollPane(l), BorderLayout.WEST); p1.add(new JScrollPane(d), BorderLayout.CENTER);
        t.addTab("タイトルから検索", p1);
        centerPanel.add(t); updatePanel();
    }

    private void showDeleteForm() {
        centerPanel.removeAll();
        JPanel p = new JPanel(new BorderLayout(10,10));
        DefaultListModel<Recipe> m = new DefaultListModel<>();
        for(Recipe r : allRecipeList.getRecipeList()) m.addElement(r);
        JList<Recipe> l = createList(m);
        JButton b = new JButton("選択したレシピを削除"); b.setBackground(Color.RED); b.setForeground(Color.WHITE);
        b.setOpaque(true); b.setBorderPainted(false);
        b.addActionListener(e -> {
            if(l.getSelectedValue()!=null && JOptionPane.showConfirmDialog(null, "削除しますか？")==0) {
                allRecipeList.getRecipeList().remove(l.getSelectedValue()); m.removeElement(l.getSelectedValue());
            }
        });
        p.add(new JScrollPane(l), BorderLayout.CENTER); p.add(b, BorderLayout.SOUTH);
        centerPanel.add(p); updatePanel();
    }

    private JList<Recipe> createList(DefaultListModel<Recipe> m) {
        JList<Recipe> l = new JList<>(m);
        l.setCellRenderer((list, val, idx, isS, f) -> {
            JLabel lbl = new JLabel(((Recipe)val).getTitle()); lbl.setOpaque(true);
            lbl.setBackground(isS ? COLOR_PRIMARY : Color.WHITE); lbl.setForeground(isS ? Color.WHITE : Color.BLACK);
            lbl.setBorder(new EmptyBorder(5,10,5,10)); return lbl;
        });
        return l;
    }

    private JTextArea createArea() { JTextArea a = new JTextArea(); a.setEditable(false); a.setMargin(new Insets(10,10,10,10)); return a; }
    private void updateArea(JTextArea a, Recipe r) {
        String s = r.getIngredients().stream().map(Ingredient::getName).collect(Collectors.joining(", "));
        a.setText("タイトル: " + r.getTitle() + "\nURL: " + r.getUrl() + "\n食材: " + s);
    }
    private JTextField createField() { JTextField f = new JTextField(); f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); return f; }
    private void addLeftAligned(JPanel p, JComponent c) { c.setAlignmentX(0.0f); p.add(c); }
    private void updatePanel() { centerPanel.revalidate(); centerPanel.repaint(); }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch(Exception e) {}
        SwingUtilities.invokeLater(() -> { SwingMain f = new SwingMain(); f.setLocationRelativeTo(null); f.setVisible(true); });
    }
}