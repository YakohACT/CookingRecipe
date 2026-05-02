import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

/**
 * レシピ管理アプリのメインフレーム。
 * サイドメニュー構築と中央パネルの切替、共有状態(レシピ・食材・AI設定)の保持、
 * テーマ(Light/Dark)の切替を担当する。
 */
public class SwingMain extends JFrame {

    private static final Preferences PREFS = Preferences.userNodeForPackage(SwingMain.class);
    private static final String PREF_THEME = "theme";

    /** FlatLaf がクラスパスにあるかを起動時に判定 */
    private static final boolean FLATLAF_AVAILABLE;
    static {
        boolean available;
        try {
            Class.forName("com.formdev.flatlaf.FlatLightLaf");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        FLATLAF_AVAILABLE = available;
    }

    private final AllRecipeList allRecipeList;
    private final IngredientMaster ingredientMaster;
    private final JPanel centerPanel;
    private final ApiKeyStore apiKeyStore;

    private RecipeAIService.Provider currentAiProvider = RecipeAIService.Provider.OPENAI;

    private boolean darkMode;
    private JButton themeButton;

    public SwingMain() {
        // IngredientMaster を先に初期化し、AllRecipeList(SQLite読込)の食材名解決に渡す
        ingredientMaster = new IngredientMaster();
        allRecipeList = new AllRecipeList(ingredientMaster);
        apiKeyStore = new ApiKeyStore(new SecureKeyStore(), PREFS);

        // 前回選択していたプロバイダーを復元
        String savedProvider = PREFS.get("aiProvider", "");
        if (!savedProvider.isEmpty()) {
            try {
                currentAiProvider = RecipeAIService.Provider.valueOf(savedProvider);
            } catch (IllegalArgumentException ignore) {}
        }

        darkMode = "dark".equals(PREFS.get(PREF_THEME, "light"));

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

        // FlatLafが入っていればテーマ切替ボタンを差し込む
        if (FLATLAF_AVAILABLE) {
            themeButton = createSideButton(themeButtonLabel(), e -> toggleTheme());
            sideMenu.add(themeButton);
            sideMenu.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        addSideButton(sideMenu, "保存して終了", e -> {
            allRecipeList.write();
            System.exit(0);
        });

        return sideMenu;
    }

    private void addSideButton(JPanel parent, String text, java.awt.event.ActionListener action) {
        JButton btn = createSideButton(text, action);
        parent.add(btn);
        parent.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private JButton createSideButton(String text, java.awt.event.ActionListener action) {
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
        return btn;
    }

    private String themeButtonLabel() {
        return darkMode ? "☀ ライトモード" : "☾ ダークモード";
    }

    /** Light/Dark を切替えてコンポーネントツリー全体を再描画する */
    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
        PREFS.put(PREF_THEME, darkMode ? "dark" : "light");
        if (themeButton != null) themeButton.setText(themeButtonLabel());
    }

    /** FlatLaf を切替える(クラスパスに無い場合は何もしない) */
    private static void applyTheme(boolean dark) {
        if (!FLATLAF_AVAILABLE) return;
        try {
            if (dark) com.formdev.flatlaf.FlatDarkLaf.setup();
            else      com.formdev.flatlaf.FlatLightLaf.setup();
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
        JLabel label = new JLabel("<html><center>Welcome!<br>操作を選択してください</center></html>", SwingConstants.CENTER);
        label.setFont(Theme.FONT_TITLE);
        welcome.add(label, BorderLayout.CENTER);
        showPanel(welcome);
    }

    // 共有状態へのアクセサ -------------------------------------------------
    public AllRecipeList getAllRecipeList() { return allRecipeList; }
    public IngredientMaster getIngredientMaster() { return ingredientMaster; }
    public RecipeAIService.Provider getCurrentAiProvider() { return currentAiProvider; }
    public void setCurrentAiProvider(RecipeAIService.Provider p) {
        this.currentAiProvider = p;
        if (p != null) PREFS.put("aiProvider", p.name());
    }
    /** 現在選択中プロバイダーの復号済みAPIキーを返す */
    public String getCurrentApiKey() { return apiKeyStore.getKey(currentAiProvider); }
    /** 指定プロバイダーのAPIキー(復号済み) */
    public String getApiKeyFor(RecipeAIService.Provider p) { return apiKeyStore.getKey(p); }
    /** 指定プロバイダーのAPIキーを暗号化して保存(空文字なら削除) */
    public void setApiKeyFor(RecipeAIService.Provider p, String plainKey) { apiKeyStore.setKey(p, plainKey); }

    /**
     * 現在選択中プロバイダーの利用モデル名を返す。
     * 保存値が無い場合はそのプロバイダーの先頭モデル(getAvailableModels()[0])にフォールバック
     */
    public String getCurrentModelName() { return getModelFor(currentAiProvider); }

    public String getModelFor(RecipeAIService.Provider p) {
        if (p == null) return "";
        String saved = PREFS.get("aiModel." + p.name(), "");
        if (!saved.isEmpty()) return saved;
        // 未設定のときはそのプロバイダーの最初の利用可能モデルにフォールバック
        String[] models = new RecipeAIService().getModelsForProvider(p);
        return models.length > 0 ? models[0] : "";
    }

    /** 指定プロバイダーの利用モデル名を保存する */
    public void setModelFor(RecipeAIService.Provider p, String model) {
        if (p == null) return;
        if (model == null || model.isEmpty()) {
            PREFS.remove("aiModel." + p.name());
        } else {
            PREFS.put("aiModel." + p.name(), model);
        }
    }

    /** 互換用: 現在のプロバイダーに対してモデルを保存 */
    public void setCurrentModelName(String m) { setModelFor(currentAiProvider, m); }

    public static void main(String[] args) {
        // 起動時に保存されたテーマを適用(無ければLight)
        boolean dark = "dark".equals(PREFS.get(PREF_THEME, "light"));
        if (FLATLAF_AVAILABLE) {
            applyTheme(dark);
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                // 失敗しても既定の Look&Feel で起動する
            }
        }

        SwingUtilities.invokeLater(() -> {
            SwingMain frame = new SwingMain();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
