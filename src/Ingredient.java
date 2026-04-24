import java.io.Serializable;
import java.util.Objects;

/**
 * 1つの食材データを管理するクラス
 * ファイルに保存できるように Serializable を実装する
 */
public class Ingredient implements Serializable {
    /** 食材名 */
    private String name;
    /** 食材のカテゴリ */
    private IngredientCategory category;

    /**
     * 新しい食材オブジェクトを作成するコンストラクタ
     * * @param name     食材名
     * @param category 食材のカテゴリ
     */
    public Ingredient(String name, IngredientCategory category) {
        this.name = name;
        this.category = category;
    }

    /**
     * 食材名を取得する
     * * @return 食材の名前の文字列
     */
    public String getName() {
        return name;
    }

    /**
     * 食材のカテゴリを取得する
     * * @return 食材のカテゴリ（IngredientCategory）
     */
    public IngredientCategory getCategory() {
        return category;
    }

    /**
     * 画面に表示する際の文字列表現を定義する
     * * @return 「食材名 (カテゴリ名)」の形式の文字列
     */
    @Override
    public String toString() {
        return name + " (" + category + ")";
    }

    /**
     * 2つの食材オブジェクトが同じ食材かどうかを名前で判定する
     * * @param o 比較対象のオブジェクト
     * @return 同じ食材であれば true、そうでなければ false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(name, that.name);
    }

    /**
     * オブジェクトのハッシュコードを返す（equalsをオーバーライドしたため必須）
     * * @return 食材名に基づくハッシュコード
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}