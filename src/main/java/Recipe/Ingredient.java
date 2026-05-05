package main.java.Recipe;

import java.io.Serializable;
import java.util.Objects;

/**
 * 食材データを管理するクラス
 * ファイル保存のため Serializable を実装
 */
public class Ingredient implements Serializable {
    private String name;
    private IngredientCategory category;

    /**
     * コンストラクタ
     * @param name 食材名
     * @param category 食材カテゴリ
     */
    public Ingredient(String name, IngredientCategory category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public IngredientCategory getCategory() { return category; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}