import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * レシピ管理アプリのメインフレーム。
 * サイドメニュー構築と中央パネルの切替、共有状態(レシピ・食材・AI設定)の保持のみを担当する
 */
public class SwingMain extends JFrame {
    private final AllRecipeList allRecipeList;
    private final IngredientMaster ingredientMaster;
    private final JPanel centerPanel;

    private RecipeAIService.Provider currentAiProvider = RecipeAIService.Provider.OPENAI;
    private String currentApiKey = "";
    private String currentModelName = "gpt-4o-mini";

    public SwingMain() {
        // IngredientMaster を先に初期化し、AllRecipeList(SQLite読込)の食材名解決に渡す
        ingredientMaster = new IngredientMaster();
        allRecipeList = new AllRecipeList(ingredientMaster);

        setTitle("Recipe Manager Pro");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                allRecipeList.write();
                System.exit(0);
            }
        });

        add(buildSideMenu(), BorderLayout.WEST);

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Theme.COLOR_BG);
        add(centerPanel, BorderLayout.CENTER);

        showWelcome();
    }

    private JPanel buildSideMenu() {
        JPanel sideMenu = new JPanel();
        sideMenu.setLayout(new BoxLayout(sideMenu, BoxLayout.Y_AXIS));
        sideMenu.setBackground(Theme.COLOR_SIDE);
        sideMenu.setPreferredSize(new Dimension(200, 0));
        sideMenu.setBorder(new EmptyBorder(30, 10, 10, 10));

        JLabel menuTitle = new JLabel("MENU");
        menuTitle.setForeground(Theme.COLOR_TEXT_LIGHT);
        menuTitle.setFont(Theme.FONT_TITLE);
        menuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sideMenu.add(menuTitle);
        sideMenu.add(Box.createRigidArea(new Dimension(0, 30)));

        addSideButton(sideMenu, "レシピ登録", e -> showPanel(new RegisterRecipePanel(this)));
        addSideButton(sideMenu, "レシピ閲覧", e -> showPanel(new ViewRecipePanel(this)));
        addSideButton(sideMenu, "レシピ削除", e -> showPanel(new DeleteRecipePanel(this)));
        addSideButton(sideMenu, "AI設定",   e -> showPanel(new SettingsPanel(this)));

        sideMenu.add(Box.createVerticalGlue());
        addSideButton(sideMenu, "保存して終了", e -> {
            allRecipeList.write();
            System.exit(0);
        });

        return sideMenu;
    }

    private void addSideButton(JPanel parent, String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBackground(Theme.COLOR_SIDE);
        btn.setForeground(Theme.COLOR_TEXT_LIGHT);
        btn.setFont(Theme.FONT_MAIN);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        parent.add(btn);
        parent.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /** 中央パネルを差し替える */
    public void showPanel(JPanel panel) {
        centerPanel.removeAll();
        centerPanel.add(panel, BorderLayout.CENTER);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /** ウェルカム画面を中央パネルに表示する */
    public void showWelcome() {
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setBackground(Theme.COLOR_BG);
        JLabel label = new JLabel("<html><center>Welcome!<br>操作を選択してください</center></html>", SwingConstants.CENTER);
        label.setFont(Theme.FONT_TITLE);
        welcome.add(label, BorderLayout.CENTER);
        showPanel(welcome);
    }

    // 共有状態へのアクセサ -------------------------------------------------
    public AllRecipeList getAllRecipeList() { return allRecipeList; }
    public IngredientMaster getIngredientMaster() { return ingredientMaster; }
    public RecipeAIService.Provider getCurrentAiProvider() { return currentAiProvider; }
    public void setCurrentAiProvider(RecipeAIService.Provider p) { this.currentAiProvider = p; }
    public String getCurrentApiKey() { return currentApiKey; }
    public void setCurrentApiKey(String k) { this.currentApiKey = k; }
    public String getCurrentModelName() { return currentModelName; }
    public void setCurrentModelName(String m) { this.currentModelName = m; }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            SwingMain frame = new SwingMain();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
