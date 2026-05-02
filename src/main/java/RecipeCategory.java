/**
 * レシピのカテゴリーを定義する列挙型。
 * 1つのレシピは複数のカテゴリーを持ちうる(例: 親子丼 = ごはんもの + 卵料理 + 肉料理 + 和食)。
 * UI表示には日本語ラベル、永続化には enum 名(英語)を使う
 */
public enum RecipeCategory {
    RICE("ごはんもの"),
    MEAT("肉料理"),
    VEGETABLE("野菜料理"),
    EGG("卵料理"),
    NOODLE("麺類"),
    SOUP("汁物"),
    BREAD("パン"),
    SEAFOOD("魚介料理"),
    HOTPOT("鍋料理"),
    FRIED("揚げ物"),
    WASHOKU("和食"),
    YOSHOKU("洋食"),
    CHUKA("中華料理"),
    SWEETS("お菓子"),
    OTHER("その他");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
