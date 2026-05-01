import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

/**
 * レシピ閲覧画面パネル（タイトル検索/食材検索の2タブ構成）
 */
public class ViewRecipePanel extends JPanel {

    private final SwingMain owner;

    public ViewRecipePanel(SwingMain owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_MAIN);
        tabs.addTab("タイトルから検索", buildTitleTab());
        tabs.addTab("食材から検索", buildIngredientTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildTitleTab() {
        JPanel titleTab = new JPanel(new BorderLayout(20, 20));
        titleTab.setBorder(new EmptyBorder(20, 20, 20, 20));
        titleTab.setBackground(Color.WHITE);

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        for (Recipe r : owner.getAllRecipeList().getRecipeList()) recipeListModel.addElement(r);

        JList<Recipe> recipeListView = UIComponents.createStyledRecipeList(recipeListModel);
        JTextArea detailArea = UIComponents.createDetailArea();

        recipeListView.addListSelectionListener(e -> {
            Recipe selected = recipeListView.getSelectedValue();
            if (selected != null) UIComponents.updateDetailArea(detailArea, selected);
        });

        titleTab.add(new JScrollPane(recipeListView), BorderLayout.WEST);
        titleTab.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        return titleTab;
    }

    private JPanel buildIngredientTab() {
        JPanel ingTab = new JPanel(new BorderLayout(20, 20));
        ingTab.setBackground(Color.WHITE);
        ingTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Ingredient> searchCondModel = new DefaultListModel<>();
        DefaultListModel<Ingredient> masterSearchModel = new DefaultListModel<>();
        DefaultListModel<Recipe> resultListModel = new DefaultListModel<>();

        JList<Ingredient> searchCondList = new JList<>(searchCondModel);
        JList<Ingredient> masterSearchList = new JList<>(masterSearchModel);
        JList<Recipe> resultListView = UIComponents.createStyledRecipeList(resultListModel);
        JTextArea searchDetailArea = UIComponents.createDetailArea();

        JComboBox<IngredientCategory> searchCatCombo = new JComboBox<>(IngredientCategory.values());
        searchCatCombo.setFont(Theme.FONT_MAIN);
        searchCatCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        searchCatCombo.addActionListener(e -> {
            masterSearchModel.clear();
            for (Ingredient ing : owner.getIngredientMaster().searchIngredient((IngredientCategory) searchCatCombo.getSelectedItem())) {
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

        resultListView.addListSelectionListener(e -> {
            Recipe selected = resultListView.getSelectedValue();
            if (selected != null) UIComponents.updateDetailArea(searchDetailArea, selected);
        });

        JButton btnSearch = UIComponents.createPrimaryButton("この食材をすべて含むレシピを検索");
        btnSearch.addActionListener(e -> {
            ArrayList<Ingredient> targets = new ArrayList<>();
            for (int i = 0; i < searchCondModel.size(); i++) targets.add(searchCondModel.getElementAt(i));

            resultListModel.clear();
            for (Recipe r : owner.getAllRecipeList().getRecipeList()) {
                if (r.getIngredients().containsAll(targets)) resultListModel.addElement(r);
            }
        });

        JPanel searchToolBox = new JPanel();
        searchToolBox.setLayout(new BoxLayout(searchToolBox, BoxLayout.Y_AXIS));
        searchToolBox.setBackground(Color.WHITE);
        searchToolBox.setPreferredSize(new Dimension(300, 0));

        UIComponents.addLeftAligned(searchToolBox, new JLabel("1. カテゴリ選択:"));
        UIComponents.addLeftAligned(searchToolBox, searchCatCombo);
        searchToolBox.add(new JScrollPane(masterSearchList));
        UIComponents.addLeftAligned(searchToolBox, btnAddCond);
        searchToolBox.add(Box.createRigidArea(new Dimension(0, 10)));
        UIComponents.addLeftAligned(searchToolBox, new JLabel("2. 現在の検索条件:"));
        searchToolBox.add(new JScrollPane(searchCondList));
        UIComponents.addLeftAligned(searchToolBox, btnClearCond);
        searchToolBox.add(Box.createRigidArea(new Dimension(0, 10)));
        UIComponents.addLeftAligned(searchToolBox, btnSearch);

        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.add(new JScrollPane(resultListView), BorderLayout.WEST);
        resultPanel.add(new JScrollPane(searchDetailArea), BorderLayout.CENTER);

        ingTab.add(searchToolBox, BorderLayout.WEST);
        ingTab.add(resultPanel, BorderLayout.CENTER);
        return ingTab;
    }
}
