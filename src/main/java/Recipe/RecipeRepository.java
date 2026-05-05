package main.java.Recipe;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite を使ってレシピを永続化するリポジトリ。
 * 3テーブル構成:
 * - recipes(id, title, url)
 * - recipe_categories(recipe_id, category)  ※ enum 名(英語)を保存
 * - recipe_ingredients(recipe_id, ingredient_name, sort_order)
 *
 * 実行時には sqlite-jdbc がクラスパスに必要 (org.xerial:sqlite-jdbc)。
 */
public class RecipeRepository {

    static {
        // SPI による自動登録が効かない環境(クラスパスJAR以外で起動された場合など)に備えて明示ロード
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[エラー] sqlite-jdbc が見つかりません。"
                    + "lib/sqlite-jdbc-3.45.3.0.jar をクラスパスに追加してください。");
        }
    }

    private final String dbPath;
    private final IngredientMaster ingredientMaster;

    public RecipeRepository(String dbPath, IngredientMaster ingredientMaster) {
        this.dbPath = dbPath;
        this.ingredientMaster = ingredientMaster;
        ensureSchema();
        // 実体ファイルが分かりにくいため、絶対パスをコンソールに表示する
        System.out.println("[main.java.Recipe.RecipeRepository] SQLite DB: " + new java.io.File(dbPath).getAbsolutePath());
    }

    /**
     * recipes.db の探索順:
     * カレント → CookingRecipe/ → 親ディレクトリ。見つからなければカレントに新規作成する
     */
    public static String resolveDbPath() {
        String[] candidates = {
                "recipes.db",
                "CookingRecipe/recipes.db",
                "../recipes.db",
                "../../recipes.db"
        };
        for (String p : candidates) {
            if (new File(p).exists()) return p;
        }
        return "recipes.db";
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private void ensureSchema() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS recipes (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  title TEXT NOT NULL," +
                    "  url TEXT NOT NULL" +
                    ")");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS recipe_categories (" +
                    "  recipe_id INTEGER NOT NULL," +
                    "  category  TEXT NOT NULL," +
                    "  PRIMARY KEY (recipe_id, category)," +
                    "  FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                    ")");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS recipe_ingredients (" +
                    "  recipe_id       INTEGER NOT NULL," +
                    "  ingredient_name TEXT NOT NULL," +
                    "  sort_order      INTEGER NOT NULL," +
                    "  PRIMARY KEY (recipe_id, sort_order)," +
                    "  FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                    ")");
        } catch (SQLException e) {
            throw new RuntimeException("SQLiteの初期化に失敗しました(sqlite-jdbcがクラスパスにあるか確認してください): " + e.getMessage(), e);
        }
    }

    /**
     * 全レシピを読み込む。カテゴリーと食材は別テーブルから結合して復元する
     */
    public List<Recipe> findAll() {
        Map<Long, RecipeBuilder> builders = new LinkedHashMap<>();
        try (Connection conn = connect()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, title, url FROM recipes ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    builders.put(id, new RecipeBuilder(id, rs.getString("title"), rs.getString("url")));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT recipe_id, category FROM recipe_categories");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecipeBuilder b = builders.get(rs.getLong("recipe_id"));
                    if (b == null) continue;
                    try {
                        b.categories.add(RecipeCategory.valueOf(rs.getString("category")));
                    } catch (IllegalArgumentException ignore) {
                        // 旧バージョンの enum 名が残っていた場合は無視
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT recipe_id, ingredient_name FROM recipe_ingredients ORDER BY recipe_id, sort_order");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecipeBuilder b = builders.get(rs.getLong("recipe_id"));
                    if (b == null) continue;
                    b.ingredients.add(lookupIngredient(rs.getString("ingredient_name")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("レシピの読み込みに失敗: " + e.getMessage(), e);
        }

        List<Recipe> result = new ArrayList<>();
        for (RecipeBuilder b : builders.values()) {
            EnumSet<RecipeCategory> cats = b.categories.isEmpty()
                    ? EnumSet.of(RecipeCategory.OTHER) : b.categories;
            Recipe r = new Recipe(b.title, b.url, b.ingredients, cats);
            r.setId(b.id);
            result.add(r);
        }
        return result;
    }

    /**
     * レシピを新規挿入し、生成された id を返す
     */
    public long insert(Recipe recipe) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipes(title, url) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, recipe.getTitle());
                ps.setString(2, recipe.getUrl());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipe_categories(recipe_id, category) VALUES (?, ?)")) {
                for (RecipeCategory cat : recipe.getCategories()) {
                    ps.setLong(1, id);
                    ps.setString(2, cat.name());
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipe_ingredients(recipe_id, ingredient_name, sort_order) VALUES (?, ?, ?)")) {
                int order = 0;
                for (Ingredient ing : recipe.getIngredients()) {
                    ps.setLong(1, id);
                    ps.setString(2, ing.getName());
                    ps.setInt(3, order++);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("レシピの保存に失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 既存レシピを更新する。タイトル/URLは UPDATE、関連テーブル(カテゴリー・食材)は
     * 全削除してから再挿入することで内容差分を反映する。1トランザクションで原子的に行う
     */
    public void update(Recipe recipe) {
        long id = recipe.getId();
        if (id <= 0) {
            throw new IllegalArgumentException("main.java.Recipe.Recipe has no id; use insert() instead");
        }
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE recipes SET title = ?, url = ? WHERE id = ?")) {
                ps.setString(1, recipe.getTitle());
                ps.setString(2, recipe.getUrl());
                ps.setLong(3, id);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_categories WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipe_categories(recipe_id, category) VALUES (?, ?)")) {
                for (RecipeCategory cat : recipe.getCategories()) {
                    ps.setLong(1, id);
                    ps.setString(2, cat.name());
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_ingredients WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO recipe_ingredients(recipe_id, ingredient_name, sort_order) VALUES (?, ?, ?)")) {
                int order = 0;
                for (Ingredient ing : recipe.getIngredients()) {
                    ps.setLong(1, id);
                    ps.setString(2, ing.getName());
                    ps.setInt(3, order++);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("レシピ更新失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 指定 id のレシピを削除する。関連テーブルは ON DELETE CASCADE が設定されているが、
     * SQLite の foreign_keys プラグマがコネクション毎にONにされるため明示的に消す
     */
    public void delete(long id) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_categories WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_ingredients WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipes WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("レシピの削除に失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 食材名から main.java.main.java.Recipe.Recipe.IngredientMaster を引いて main.java.main.java.Recipe.Recipe.Ingredient を返す。
     * マスタに存在しない場合は category を OTHER にした暫定オブジェクトを返す
     */
    private Ingredient lookupIngredient(String name) {
        for (Ingredient ing : ingredientMaster.getAllIngredients()) {
            if (ing.getName().equals(name)) return ing;
        }
        return new Ingredient(name, IngredientCategory.OTHER);
    }

    /** 読み込み中の中間状態を保持する内部Builder */
    private static class RecipeBuilder {
        final long id;
        final String title;
        final String url;
        final ArrayList<Ingredient> ingredients = new ArrayList<>();
        final EnumSet<RecipeCategory> categories = EnumSet.noneOf(RecipeCategory.class);

        RecipeBuilder(long id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }
    }
}
