import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * レシピ情報を管理するクラス。
 * 1つのレシピは複数のカテゴリーを持つことができる。
 */
public class Recipe implements Serializable {
    private static final long serialVersionUID = 2L;

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

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public ArrayList<Ingredient> getIngredients() { return ingredients; }

    /** categories が null(旧データから読み込んだ場合)のときは OTHER のみのSetを返す */
    public EnumSet<RecipeCategory> getCategories() {
        if (categories == null || categories.isEmpty()) return EnumSet.of(RecipeCategory.OTHER);
        return EnumSet.copyOf(categories);
    }
}
