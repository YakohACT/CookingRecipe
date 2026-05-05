package main.java.Recipe;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * レシピ情報を管理するクラス。
 * 1つのレシピは複数のカテゴリーを持つことができる。
 * 永続化は SQLite が担当するため Serializable は実装しない。
 * id はDB主キー(0は未永続化を表す)。
 */
public class Recipe {

    private long id;
    private String title;
    private String url;
    private ArrayList<Ingredient> ingredients;
    private EnumSet<RecipeCategory> categories;

    public Recipe(String title, String url, ArrayList<Ingredient> ingredients, EnumSet<RecipeCategory> categories) {
        this.title = title;
        this.url = url;
        this.ingredients = ingredients;
        this.categories = (categories == null || categories.isEmpty())
                ? EnumSet.of(RecipeCategory.OTHER)
                : EnumSet.copyOf(categories);
    }

    /** カテゴリー未指定時のフォールバックを許容する旧形式コンストラクタ */
    public Recipe(String title, String url, ArrayList<Ingredient> ingredients) {
        this(title, url, ingredients, EnumSet.of(RecipeCategory.OTHER));
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public ArrayList<Ingredient> getIngredients() { return ingredients; }

    public EnumSet<RecipeCategory> getCategories() {
        if (categories == null || categories.isEmpty()) return EnumSet.of(RecipeCategory.OTHER);
        return EnumSet.copyOf(categories);
    }
}
