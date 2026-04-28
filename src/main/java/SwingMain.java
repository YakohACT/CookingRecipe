import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * レシピ管理システムのUIを管理するSwing用メインクラス
 */
public class SwingMain extends JFrame {
    private AllRecipeList allRecipeList;
    private IngredientMaster ingredientMaster;
    private JPanel centerPanel;

    // AI設定保持用の変数
    private RecipeAIService.Provider currentAiProvider = RecipeAIService.Provider.OPENAI;
    private String currentApiKey = "";

    // カラーテーマの定義
    private final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private final Color COLOR_BG = new Color(245, 247, 250);
    private final Color COLOR_SIDE = new Color(44, 62, 80);
    private final Color COLOR_TEXT_LIGHT = Color.WHITE;
    private final Font FONT_MAIN = new Font("SansSerif", Font.PLAIN, 14);
    private final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);

    /**
     * GUIの初期構築を行うコンストラクタ
     */
    public SwingMain() {
        allRecipeList = new AllRecipeList();
        ingredientMaster = new IngredientMaster();

        setTitle("Recipe Manager Pro");
        setSize(1000, 750);

        // ウィンドウ終了時の動作設定（保存してから終了）
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                allRecipeList.write();
                System.exit(0);
            }
        });

        // サイドメニューの構築
        JPanel sideMenu = new JPanel();
        sideMenu.setLayout(new BoxLayout(sideMenu, BoxLayout.Y_AXIS));
        sideMenu.setBackground(COLOR_SIDE);
        sideMenu.setPreferredSize(new Dimension(200, 0));
        sideMenu.setBorder(new EmptyBorder(30, 10, 10, 10));

        JLabel menuTitle = new JLabel("MENU");
        menuTitle.setForeground(COLOR_TEXT_LIGHT);
        menuTitle.setFont(FONT_TITLE);
        menuTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sideMenu.add(menuTitle);
        sideMenu.add(Box.createRigidArea(new Dimension(0, 30)));

        addSideButton(sideMenu, "レシピ登録", e -> showRegisterForm());
        addSideButton(sideMenu, "レシピ閲覧", e -> showViewMenu());
        addSideButton(sideMenu, "レシピ削除", e -> showDeleteForm());
        addSideButton(sideMenu, "AI設定", e -> showSettingsForm());

        sideMenu.add(Box.createVerticalGlue());
        addSideButton(sideMenu, "保存して終了", e -> {
            allRecipeList.write();
            System.exit(0);
        });

        add(sideMenu, BorderLayout.WEST);

        // 中央パネルの設定
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_BG);
        add(centerPanel, BorderLayout.CENTER);

        showWelcomeMessage();
    }

    /**
     * サイドメニュー用ボタンの追加
     * @param parent 追加先パネル
     * @param text ボタンのテキスト
     * @param action クリック時のアクション
     */
    private void addSideButton(JPanel parent, String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBackground(COLOR_SIDE);
        btn.setForeground(COLOR_TEXT_LIGHT);
        btn.setFont(FONT_MAIN);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        parent.add(btn);
        parent.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     * 初期画面の表示
     */
    private void showWelcomeMessage() {
        centerPanel.removeAll();
        JLabel label = new JLabel("<html><center>Welcome!<br>操作を選択してください</center></html>", SwingConstants.CENTER);
        label.setFont(FONT_TITLE);
        centerPanel.add(label, BorderLayout.CENTER);
        updatePanel();
    }

    /**
     * AIプロバイダーとAPIキーの設定画面を表示
     */
    private void showSettingsForm() {
        centerPanel.removeAll();
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("AI利用設定");
        titleLabel.setFont(FONT_TITLE);
        addLeftAligned(form, titleLabel);
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        addLeftAligned(form, new JLabel("利用するAIプロバイダーを選択してください:"));
        JComboBox<RecipeAIService.Provider> providerCombo = new JComboBox<>(RecipeAIService.Provider.values());
        providerCombo.setSelectedItem(currentAiProvider);
        providerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        addLeftAligned(form, providerCombo);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        addLeftAligned(form, new JLabel("APIキーを入力してください:"));
        JPasswordField keyField = new JPasswordField(currentApiKey);
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        addLeftAligned(form, keyField);

        form.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton btnSave = new JButton("設定を保存");
        btnSave.setBackground(COLOR_PRIMARY);
        btnSave.setForeground(Color.WHITE);
        btnSave.setOpaque(true);
        btnSave.setBorderPainted(false);
        btnSave.addActionListener(e -> {
            currentAiProvider = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            currentApiKey = new String(keyField.getPassword()).trim();
            JOptionPane.showMessageDialog(this, "設定を保存しました");
            showWelcomeMessage();
        });
        addLeftAligned(form, btnSave);

        centerPanel.add(new JScrollPane(form), BorderLayout.CENTER);
        updatePanel();
    }

    /**
     * AI機能・入力チェック付き登録フォームの表示
     */
    private void showRegisterForm() {
        centerPanel.removeAll();

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        // タイトルとAIボタンのエリア
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("新規レシピ登録");
        titleLabel.setFont(FONT_TITLE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton btnAi = new JButton("✨ AI自動提案");
        btnAi.setBackground(new Color(155, 89, 182));
        btnAi.setForeground(Color.WHITE);
        btnAi.setOpaque(true);
        btnAi.setBorderPainted(false);
        btnAi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerPanel.add(btnAi, BorderLayout.EAST);

        form.add(headerPanel);
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        JTextField titleField = createStyledTextField();
        JTextField urlField = createStyledTextField();

        JComboBox<IngredientCategory> catCombo = new JComboBox<>(IngredientCategory.values());
        catCombo.setFont(FONT_MAIN);
        catCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        DefaultListModel<Ingredient> masterListModel = new DefaultListModel<>();
        JList<Ingredient> masterIngList = new JList<>(masterListModel);

        DefaultListModel<Ingredient> selectedListModel = new DefaultListModel<>();
        JList<Ingredient> selectedIngList = new JList<>(selectedListModel);

        catCombo.addActionListener(e -> {
            IngredientCategory cat = (IngredientCategory) catCombo.getSelectedItem();
            masterListModel.clear();
            if (cat != null) {
                for (Ingredient ing : ingredientMaster.searchIngredient(cat)) {
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

        // AIボタンのアクション
        btnAi.addActionListener(e -> {
            if (currentApiKey.isEmpty()) {
                JOptionPane.showMessageDialog(this, "先に「AI設定」メニューからAPIキーを入力してください");
                return;
            }
            btnAi.setText("生成中...");
            btnAi.setEnabled(false);

            SwingWorker<String[], Void> worker = new SwingWorker<String[], Void>() {
                @Override
                protected String[] doInBackground() throws Exception {
                    RecipeAIService aiService = new RecipeAIService();
                    aiService.setConfig(currentAiProvider, currentApiKey);
                    return aiService.suggestRecipe(ingredientMaster.getAllIngredients());
                }

                @Override
                protected void done() {
                    try {
                        String[] result = get();
                        titleField.setText(result[0]);
                        selectedListModel.clear();

                        String[] aiIngs = result[1].split(",");
                        for (String aiIngName : aiIngs) {
                            String cleanName = aiIngName.trim();
                            for (Ingredient ing : ingredientMaster.getAllIngredients()) {
                                if (ing.getName().equals(cleanName) && !selectedListModel.contains(ing)) {
                                    selectedListModel.addElement(ing);
                                }
                            }
                        }
                        JOptionPane.showMessageDialog(SwingMain.this, "AIがレシピを提案しました");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(SwingMain.this, "AIの呼び出しに失敗しました。\nAPIキーやインターネット接続を確認してください。", "エラー", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnAi.setText("✨ AI自動提案");
                        btnAi.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        JButton btnSubmit = new JButton("レシピを保存");
        btnSubmit.setBackground(COLOR_PRIMARY);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setOpaque(true);
        btnSubmit.setBorderPainted(false);

        btnSubmit.addActionListener(e -> {
            String title = titleField.getText().trim();
            String url = urlField.getText().trim();

            // バリデーションチェック
            if (title.isEmpty() || url.isEmpty() || selectedListModel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "入力されていない箇所があります", "入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ArrayList<Ingredient> ings = new ArrayList<>();
            for (int i = 0; i < selectedListModel.size(); i++) ings.add(selectedListModel.getElementAt(i));
            allRecipeList.addRecipe(new Recipe(title, url, ings));
            JOptionPane.showMessageDialog(this, "レシピ「" + title + "」を登録しました");
            showWelcomeMessage();
        });

        // コンポーネントの配置
        addLeftAligned(form, new JLabel("タイトル:"));
        addLeftAligned(form, titleField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        addLeftAligned(form, new JLabel("URL (AI生成時は手動で入力してください):"));
        addLeftAligned(form, urlField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));
        addLeftAligned(form, new JLabel("カテゴリから食材を選択:"));
        addLeftAligned(form, catCombo);

        JScrollPane mScroll = new JScrollPane(masterIngList);
        mScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(mScroll);
        addLeftAligned(form, btnAdd);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        addLeftAligned(form, new JLabel("使用する食材:"));
        JScrollPane sScroll = new JScrollPane(selectedIngList);
        sScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sScroll);

        form.add(Box.createRigidArea(new Dimension(0, 20)));
        addLeftAligned(form, btnSubmit);

        centerPanel.add(new JScrollPane(form), BorderLayout.CENTER);
        updatePanel();
    }

    /**
     * 複数検索パターンを持つ閲覧画面の表示
     */
    private void showViewMenu() {
        centerPanel.removeAll();
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_MAIN);

        // --- タブ1: タイトルからの検索 ---
        JPanel titleTab = new JPanel(new BorderLayout(20, 20));
        titleTab.setBorder(new EmptyBorder(20, 20, 20, 20));
        titleTab.setBackground(Color.WHITE);

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        for (Recipe r : allRecipeList.getRecipeList()) recipeListModel.addElement(r);

        JList<Recipe> recipeListView = createStyledRecipeList(recipeListModel);
        JTextArea detailArea = createDetailArea();

        recipeListView.addListSelectionListener(e -> {
            Recipe selected = recipeListView.getSelectedValue();
            if (selected != null) updateDetailArea(detailArea, selected);
        });

        titleTab.add(new JScrollPane(recipeListView), BorderLayout.WEST);
        titleTab.add(new JScrollPane(detailArea), BorderLayout.CENTER);

        // --- タブ2: 食材からの検索 ---
        JPanel ingTab = new JPanel(new BorderLayout(20, 20));
        ingTab.setBackground(Color.WHITE);
        ingTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel searchToolBox = new JPanel();
        searchToolBox.setLayout(new BoxLayout(searchToolBox, BoxLayout.Y_AXIS));
        searchToolBox.setBackground(Color.WHITE);
        searchToolBox.setPreferredSize(new Dimension(300, 0));

        DefaultListModel<Ingredient> searchCondModel = new DefaultListModel<>();
        JList<Ingredient> searchCondList = new JList<>(searchCondModel);

        JComboBox<IngredientCategory> searchCatCombo = new JComboBox<>(IngredientCategory.values());
        searchCatCombo.setFont(FONT_MAIN);
        searchCatCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        DefaultListModel<Ingredient> masterSearchModel = new DefaultListModel<>();
        JList<Ingredient> masterSearchList = new JList<>(masterSearchModel);

        searchCatCombo.addActionListener(e -> {
            masterSearchModel.clear();
            for (Ingredient ing : ingredientMaster.searchIngredient((IngredientCategory)searchCatCombo.getSelectedItem())) {
                masterSearchModel.addElement(ing);
            }
        });

        JButton btnAddCond = new JButton("検索条件に追加");
        btnAddCond.addActionListener(e -> {
            Ingredient selected = masterSearchList.getSelectedValue();
            if (selected != null && !searchCondModel.contains(selected)) searchCondModel.addElement(selected);
        });

        JButton btnClearCond = new JButton("条件をクリア");
        btnClearCond.addActionListener(e -> searchCondModel.clear());

        DefaultListModel<Recipe> resultListModel = new DefaultListModel<>();
        JList<Recipe> resultListView = createStyledRecipeList(resultListModel);
        JTextArea searchDetailArea = createDetailArea();

        resultListView.addListSelectionListener(e -> {
            Recipe selected = resultListView.getSelectedValue();
            if (selected != null) updateDetailArea(searchDetailArea, selected);
        });

        JButton btnSearch = new JButton("この食材をすべて含むレシピを検索");
        btnSearch.setBackground(COLOR_PRIMARY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setOpaque(true);
        btnSearch.setBorderPainted(false);
        btnSearch.addActionListener(e -> {
            ArrayList<Ingredient> targets = new ArrayList<>();
            for (int i = 0; i < searchCondModel.size(); i++) targets.add(searchCondModel.getElementAt(i));

            resultListModel.clear();
            for (Recipe r : allRecipeList.getRecipeList()) {
                if (r.getIngredients().containsAll(targets)) resultListModel.addElement(r);
            }
        });

        addLeftAligned(searchToolBox, new JLabel("1. カテゴリ選択:"));
        addLeftAligned(searchToolBox, searchCatCombo);
        searchToolBox.add(new JScrollPane(masterSearchList));
        addLeftAligned(searchToolBox, btnAddCond);
        searchToolBox.add(Box.createRigidArea(new Dimension(0, 10)));
        addLeftAligned(searchToolBox, new JLabel("2. 現在の検索条件:"));
        searchToolBox.add(new JScrollPane(searchCondList));
        addLeftAligned(searchToolBox, btnClearCond);
        searchToolBox.add(Box.createRigidArea(new Dimension(0, 10)));
        addLeftAligned(searchToolBox, btnSearch);

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.add(new JScrollPane(resultListView), BorderLayout.WEST);
        resultPanel.add(new JScrollPane(searchDetailArea), BorderLayout.CENTER);

        ingTab.add(searchToolBox, BorderLayout.WEST);
        ingTab.add(resultPanel, BorderLayout.CENTER);

        tabs.addTab("タイトルから検索", titleTab);
        tabs.addTab("食材から検索", ingTab);
        centerPanel.add(tabs, BorderLayout.CENTER);
        updatePanel();
    }

    /**
     * 選択ハイライト機能付きリストの作成
     * @param model リストモデル
     * @return カスタマイズされたJList
     */
    private JList<Recipe> createStyledRecipeList(DefaultListModel<Recipe> model) {
        JList<Recipe> list = new JList<>(model);
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getTitle());
            label.setOpaque(true);
            label.setFont(FONT_MAIN);
            label.setBorder(new EmptyBorder(8, 15, 8, 15));
            if (isSelected) {
                label.setBackground(COLOR_PRIMARY);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
            return label;
        });
        list.setPreferredSize(new Dimension(200, 0));
        return list;
    }

    /**
     * レシピ詳細表示用テキストエリアの作成
     * @return カスタマイズされたJTextArea
     */
    private JTextArea createDetailArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(FONT_MAIN);
        area.setMargin(new Insets(20, 20, 20, 20));
        return area;
    }

    /**
     * 詳細表示エリアの内容更新
     * @param area 更新対象のJTextArea
     * @param recipe 表示するレシピ
     */
    private void updateDetailArea(JTextArea area, Recipe recipe) {
        String ings = recipe.getIngredients().stream().map(Ingredient::getName).collect(Collectors.joining(", "));
        area.setText("【レシピ名】\n" + recipe.getTitle() + "\n\n【URL】\n" + recipe.getUrl() + "\n\n【必要食材】\n" + ings);
    }

    /**
     * 削除画面の表示
     */
    private void showDeleteForm() {
        centerPanel.removeAll();
        JPanel box = new JPanel(new BorderLayout(10, 10));
        box.setBackground(Color.WHITE);
        box.setBorder(new EmptyBorder(30, 50, 30, 50));

        DefaultListModel<Recipe> listModel = new DefaultListModel<>();
        for (Recipe r : allRecipeList.getRecipeList()) listModel.addElement(r);
        JList<Recipe> list = createStyledRecipeList(listModel);
        list.setPreferredSize(new Dimension(0, 0));

        JButton btnDel = new JButton("選択したレシピを削除");
        btnDel.setBackground(new Color(231, 76, 60));
        btnDel.setForeground(Color.WHITE);
        btnDel.setOpaque(true);
        btnDel.setBorderPainted(false);
        btnDel.addActionListener(e -> {
            Recipe selected = list.getSelectedValue();
            if (selected != null) {
                int result = JOptionPane.showConfirmDialog(this,
                        "本当に「" + selected.getTitle() + "」を削除しますか？",
                        "削除の確認", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    allRecipeList.deleteRecipe(selected);
                    listModel.removeElement(selected);
                }
            }
        });

        JLabel label = new JLabel("削除するレシピを選択してください");
        label.setFont(FONT_MAIN);
        box.add(label, BorderLayout.NORTH);
        box.add(new JScrollPane(list), BorderLayout.CENTER);
        box.add(btnDel, BorderLayout.SOUTH);

        centerPanel.add(box, BorderLayout.CENTER);
        updatePanel();
    }

    private void addLeftAligned(JPanel panel, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(comp);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 35));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(FONT_MAIN);
        return field;
    }

    private void updatePanel() {
        centerPanel.revalidate();
        centerPanel.repaint();
    }

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