import java.io.Serializable;
import java.util.ArrayList;

/**
 * レシピ情報を管理するクラス
 */
public class Recipe implements Serializable {
    private String title;
    private String url;
    private ArrayList<Ingredient> ingredients;

    public Recipe(String title, String url, ArrayList<Ingredient> ingredients) {
        this.title = title;
        this.url = url;
        this.ingredients = ingredients;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public ArrayList<Ingredient> getIngredients() { return ingredients; }
}