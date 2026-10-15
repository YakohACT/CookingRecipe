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
import java.util.List;

/**
 * レシピ閲覧画面パネル（タイトル検索/食材検索/カテゴリ検索の3タブ構成）。
 * 各タブで選択中のレシピを編集パネルへ送る「編集」ボタンと、削除する「削除」ボタンを持つ
 */
public class ViewRecipePanel extends JPanel {

    private final SwingMain owner;
    /** 各タブのレシピ一覧モデル。削除したレシピを全タブから取り除くために保持する */
    private final List<DefaultListModel<Recipe>> recipeModels = new ArrayList<>();

    /**
     * 詳細表示エリア + 「削除」「編集」ボタンを縦に並べたパネルを生成する。
     * source で選択したレシピを詳細エリアに表示し、選択が無いときは詳細を消してボタンを無効化する。
     * @param detail 表示する詳細エリア
     * @param source 選択状態を監視する対象リスト
     * @return 詳細エリアと操作ボタンをまとめたパネル
     */
    private JPanel detailWithActionButtons(JEditorPane detail, JList<Recipe> source) {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setOpaque(false);
        container.add(new JScrollPane(detail), BorderLayout.CENTER);

        JButton btnDelete = UIComponents.createColoredButton("選択中のレシピを削除", Theme.COLOR_DELETE);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteSelected(source));

        JButton btnEdit = UIComponents.createPrimaryButton("選択中のレシピを編集");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(e -> {
            Recipe r = source.getSelectedValue();
            if (r != null) owner.showPanel(new RegisterRecipePanel(owner, r));
        });

        source.addListSelectionListener(e -> {
            Recipe selected = source.getSelectedValue();
            // 削除などで選択が外れた場合は、前のレシピが詳細に残らないよう消す
            if (selected != null) UIComponents.updateDetailArea(detail, selected);
            else detail.setText("");
            btnDelete.setEnabled(selected != null);
            btnEdit.setEnabled(selected != null);
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnDelete);
        bottom.add(Box.createRigidArea(new Dimension(8, 0)));
        bottom.add(btnEdit);
        container.add(bottom, BorderLayout.SOUTH);
        return container;
    }

    /**
     * 選択中のレシピを確認のうえ削除し、全タブの一覧から取り除く。
     * @param source 削除対象を選択しているリスト
     */
    private void deleteSelected(JList<Recipe> source) {
        Recipe selected = source.getSelectedValue();
        if (selected == null) return;
        int result = JOptionPane.showConfirmDialog(this,
                "本当に「" + selected.getTitle() + "」を削除しますか？",
                "削除の確認", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) return;

        owner.getAllRecipeList().deleteRecipe(selected);
        for (DefaultListModel<Recipe> model : recipeModels) {
            model.removeElement(selected);
        }
    }

    /**
     * 閲覧パネルを構築する。
     * @param owner 共有状態(レシピリスト等)にアクセスするためのフレーム参照
     */
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

    /**
     * タイトル一覧から選択する1つ目のタブを構築する。
     * @return タブコンテンツのパネル
     */
    private JPanel buildTitleTab() {
        JPanel titleTab = new JPanel(new BorderLayout(20, 20));
        titleTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        for (Recipe r : owner.getAllRecipeList().getRecipeList()) recipeListModel.addElement(r);
        recipeModels.add(recipeListModel);

        JList<Recipe> recipeListView = UIComponents.createStyledRecipeList(recipeListModel);
        JEditorPane detailArea = UIComponents.createDetailArea();

        titleTab.add(new JScrollPane(recipeListView), BorderLayout.WEST);
        titleTab.add(detailWithActionButtons(detailArea, recipeListView), BorderLayout.CENTER);
        return titleTab;
    }

    /**
     * 食材を AND 条件で絞り込む2つ目のタブを構築する。
     * @return タブコンテンツのパネル
     */
    private JPanel buildIngredientTab() {
        JPanel ingTab = new JPanel(new BorderLayout(20, 20));
        ingTab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Ingredient> searchCondModel = new DefaultListModel<>();
        DefaultListModel<Ingredient> masterSearchModel = new DefaultListModel<>();
        DefaultListModel<Recipe> resultListModel = new DefaultListModel<>();
        recipeModels.add(resultListModel);

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
        resultPanel.add(detailWithActionButtons(searchDetailArea, resultListView), BorderLayout.CENTER);

        ingTab.add(searchToolBox, BorderLayout.WEST);
        ingTab.add(resultPanel, BorderLayout.CENTER);
        return ingTab;
    }

    /**
     * カテゴリー選択でレシピを絞り込む3つ目のタブを構築する。
     * @return タブコンテンツのパネル
     */
    private JPanel buildCategoryTab() {
        JPanel tab = new JPanel(new BorderLayout(20, 20));
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
        recipeModels.add(recipeListModel);
        JList<Recipe> recipeListView = UIComponents.createStyledRecipeList(recipeListModel);
        JEditorPane detailArea = UIComponents.createDetailArea();

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
        tab.add(detailWithActionButtons(detailArea, recipeListView), BorderLayout.CENTER);
        return tab;
    }
}
