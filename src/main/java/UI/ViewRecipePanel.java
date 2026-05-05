package main.java.UI;

import main.java.SwingMain;
import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientCategory;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

/**
 * レシピ閲覧画面パネル（タイトル検索/食材検索/カテゴリ検索の3タブ構成）。
 * 各タブで選択中のレシピを編集パネルへ送る「編集」ボタンを持つ
 */
public class ViewRecipePanel extends JPanel {

    private final SwingMain owner;

    /**
     * 詳細表示エリア + 「編集」ボタンを縦に並べたパネルを生成する。
     * source の選択が無いときは編集ボタンが無効化される
     */
    private JPanel detailWithEditButton(JEditorPane detail, JList<Recipe> source) {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setOpaque(false);
        container.add(new JScrollPane(detail), BorderLayout.CENTER);

        JButton btnEdit = UIComponents.createPrimaryButton("選択中のレシピを編集");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(e -> {
            Recipe r = source.getSelectedValue();
            if (r != null) owner.showPanel(new RegisterRecipePanel(owner, r));
        });
        source.addListSelectionListener(e -> btnEdit.setEnabled(source.getSelectedValue() != null));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnEdit);
        container.add(bottom, BorderLayout.SOUTH);
        return container;
    }

    public ViewRecipePanel(SwingMain owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_MAIN);
        tabs.addTab("タイトルから検索", buildTitleTab());
        tabs.addTab("食材から検索", buildIngredientTab());
        tabs.addTab("カテゴリーから検索", buildCategoryTab());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildTitleTab() {
        JPanel titleTab = new JPanel(new BorderLayout(20, 20));
        titleTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        for (Recipe r : owner.getAllRecipeList().getRecipeList()) recipeListModel.addElement(r);

        JList<Recipe> recipeListView = UIComponents.createStyledRecipeList(recipeListModel);
        JEditorPane detailArea = UIComponents.createDetailArea();

        recipeListView.addListSelectionListener(e -> {
            Recipe selected = recipeListView.getSelectedValue();
            if (selected != null) UIComponents.updateDetailArea(detailArea, selected);
        });

        titleTab.add(new JScrollPane(recipeListView), BorderLayout.WEST);
        titleTab.add(detailWithEditButton(detailArea, recipeListView), BorderLayout.CENTER);
        return titleTab;
    }

    private JPanel buildIngredientTab() {
        JPanel ingTab = new JPanel(new BorderLayout(20, 20));
        ingTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Ingredient> searchCondModel = new DefaultListModel<>();
        DefaultListModel<Ingredient> masterSearchModel = new DefaultListModel<>();
        DefaultListModel<Recipe> resultListModel = new DefaultListModel<>();

        JList<Ingredient> searchCondList = new JList<>(searchCondModel);
        JList<Ingredient> masterSearchList = new JList<>(masterSearchModel);
        JList<Recipe> resultListView = UIComponents.createStyledRecipeList(resultListModel);
        JEditorPane searchDetailArea = UIComponents.createDetailArea();

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
        resultPanel.setOpaque(false);
        resultPanel.add(new JScrollPane(resultListView), BorderLayout.WEST);
        resultPanel.add(detailWithEditButton(searchDetailArea, resultListView), BorderLayout.CENTER);

        ingTab.add(searchToolBox, BorderLayout.WEST);
        ingTab.add(resultPanel, BorderLayout.CENTER);
        return ingTab;
    }

    /**
     * カテゴリー選択でレシピを絞り込む3つ目のタブ
     */
    private JPanel buildCategoryTab() {
        JPanel tab = new JPanel(new BorderLayout(20, 20));
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        JList<Recipe> recipeListView = UIComponents.createStyledRecipeList(recipeListModel);
        JEditorPane detailArea = UIComponents.createDetailArea();

        recipeListView.addListSelectionListener(e -> {
            Recipe selected = recipeListView.getSelectedValue();
            if (selected != null) UIComponents.updateDetailArea(detailArea, selected);
        });

        JComboBox<RecipeCategory> categoryCombo = new JComboBox<>(RecipeCategory.values());
        categoryCombo.setFont(Theme.FONT_MAIN);

        Runnable refresh = () -> {
            RecipeCategory selected = (RecipeCategory) categoryCombo.getSelectedItem();
            recipeListModel.clear();
            detailArea.setText("");
            if (selected != null) {
                for (Recipe r : owner.getAllRecipeList().getRecipeList()) {
                    if (r.getCategories().contains(selected)) recipeListModel.addElement(r);
                }
            }
        };
        categoryCombo.addActionListener(e -> refresh.run());
        refresh.run();

        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);
        JLabel catLabel = new JLabel("カテゴリー:");
        catLabel.setFont(Theme.FONT_MAIN);
        topRow.add(catLabel, BorderLayout.WEST);
        topRow.add(categoryCombo, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(280, 0));
        leftPanel.add(topRow, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(recipeListView), BorderLayout.CENTER);

        tab.add(leftPanel, BorderLayout.WEST);
        tab.add(detailWithEditButton(detailArea, recipeListView), BorderLayout.CENTER);
        return tab;
    }
}
