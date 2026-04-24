import java.io.Serializable;
import java.util.ArrayList;

/**
 * 1つのレシピのデータを管理するクラスです。
 * レシピのタイトル、URL、および必要な食材のリストを保持します。
 * ファイルに保存できるように Serializable を実装しています。
 */
public class Recipe implements Serializable {
    /** レシピのタイトル */
    private String title;
    /** レシピの参考URL */
    private String url;
    /** このレシピに必要な食材のリスト */
    private ArrayList<Ingredient> ingredients;

    /**
     * 新しいレシピを作成するコンストラクタです。
     * * @param title       レシピのタイトル
     * @param url         レシピの参考URL
     * @param ingredients 必要な食材のリスト
     */
    public Recipe(String title, String url, ArrayList<Ingredient> ingredients) {
        this.title = title;
        this.url = url;
        this.ingredients = ingredients;
    }

    /**
     * レシピのゲッター
     * * @return レシピのタイトル
     */
    public String getTitle() {
        return title;
    }

    /**
     * レシピのセッター
     * * @param title 新しいタイトル
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * レシピのURLのゲッター
     * * @return レシピのURL
     */
    public String getUrl() {
        return url;
    }

    /**
     * レシピのURLのセッター
     * * @param url 新しいURL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * レシピに必要な食材のリストのゲッター
     * * @return 食材のリスト
     */
    public ArrayList<Ingredient> getIngredients() {
        return ingredients;
    }

    /**
     * レシピに必要な食材のリストのセッター
     * * @param ingredients 新しい食材のリスト
     */
    public void setIngredients(ArrayList<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    /**
     * レシピに新しい食材を1つ追加する
     * 既に含まれている食材は追加されない
     * * @param ingredient 追加する食材オブジェクト
     */
    public void addIngredients(Ingredient ingredient) {
        if (!ingredients.contains(ingredient)) {
            ingredients.add(ingredient);
        }
    }

    /**
     * レシピから指定された食材を削除する
     * * @param ingredient 削除する食材オブジェクト
     */
    public void deleteIngredients(Ingredient ingredient) {
        ingredients.remove(ingredient);
    }
}