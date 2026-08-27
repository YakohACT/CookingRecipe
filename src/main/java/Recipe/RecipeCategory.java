package main.java.Recipe;

/**
 * レシピのカテゴリーを定義する列挙型
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

    /**
     * @param displayName UI表示用の日本語ラベル
     */
    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return UI表示用の日本語ラベル
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * コンボボックスで日本語表示するためdisplayNameを返す
     * @return UI表示用の日本語ラベル
     */
    @Override
    public String toString() {
        return displayName;
    }
}
