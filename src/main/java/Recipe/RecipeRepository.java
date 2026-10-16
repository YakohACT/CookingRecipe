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
import java.util.Properties;

/**
 * SQLiteを使ってレシピ保存するクラス
 * テーブル構成:
 * - recipes(id, title, url)
 * - recipe_categories(recipe_id, category)  ※ enum 名(英語)を保存
 * - recipe_ingredients(recipe_id, ingredient_name, sort_order, amount, unit)
 */
public class RecipeRepository {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[エラー] sqlite-jdbc が見つかりません。"
                    + "lib/sqlite-jdbc-3.45.3.0.jar をクラスパスに追加してください。");
        }
    }

    private final String dbPath;
    private final IngredientMaster ingredientMaster;

    /**
     * 指定パスのSQLiteを開き、必要ならテーブルを作成するメソッド
     * @param dbPath           SQLite データベースファイルのパス
     * @param ingredientMaster 読み込み時に食材名を解決するためのマスタ
     */
    public RecipeRepository(String dbPath, IngredientMaster ingredientMaster) {
        this.dbPath = dbPath;
        this.ingredientMaster = ingredientMaster;
        ensureSchema();
        // 実体ファイルが分かりにくいため、絶対パスをコンソールに表示する
        System.out.println("[RecipeRepository] SQLite DB: " + new java.io.File(dbPath).getAbsolutePath());
    }

    /**
     * recipes.db の探索順:
     * カレント → CookingRecipe/ -> 親ディレクトリ。見つからなければカレントに新規作成する
     * @return 既存ファイルのパス。どこにも無ければ "recipes.db"(カレント基準)
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

    /**
     * SQLite への新規 JDBC 接続を確立するメソッド
     * SQLite の外部キー制約(ON DELETE CASCADE)は接続ごとに有効化が必要なため、毎回 ON で接続する
     * @return 開いた状態の接続(呼び出し側で close すること)
     * @throws SQLException 接続失敗時
     */
    private Connection connect() throws SQLException {
        Properties props = new Properties();
        props.setProperty("foreign_keys", "true");
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath, props);
    }

    /**
     * 必要な3テーブル(recipes / recipe_categories / recipe_ingredients)を作成するメソッド
     * 分量の列(amount / unit)が無い古いDBには列を追加する
     */
    private void ensureSchema() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
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
                    "  amount          TEXT NOT NULL DEFAULT ''," +
                    "  unit            TEXT NOT NULL DEFAULT ''," +
                    "  PRIMARY KEY (recipe_id, sort_order)," +
                    "  FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                    ")");
            addQuantityColumnsIfMissing(conn);
        } catch (SQLException e) {
            throw new RuntimeException("SQLiteの初期化に失敗しました(sqlite-jdbcがクラスパスにあるか確認してください): " + e.getMessage(), e);
        }
    }

    /**
     * 分量の列が追加される前に作られた recipe_ingredients に amount / unit 列を追加するメソッド
     * 既存の行は分量なし(空文字)になる
     * @param conn 接続
     * @throws SQLException 列の確認/追加に失敗した場合
     */
    private void addQuantityColumnsIfMissing(Connection conn) throws SQLException {
        boolean hasAmount = false;
        boolean hasUnit = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(recipe_ingredients)")) {
            while (rs.next()) {
                String column = rs.getString("name");
                if ("amount".equals(column)) hasAmount = true;
                if ("unit".equals(column)) hasUnit = true;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            if (!hasAmount) stmt.execute("ALTER TABLE recipe_ingredients ADD COLUMN amount TEXT NOT NULL DEFAULT ''");
            if (!hasUnit) stmt.execute("ALTER TABLE recipe_ingredients ADD COLUMN unit TEXT NOT NULL DEFAULT ''");
        }
    }

    /**
     * 全レシピを読み込む。カテゴリーと食材は別テーブルから結合して復元するメソッド
     * @return 全レシピのリスト
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
                    "SELECT recipe_id, ingredient_name, amount, unit FROM recipe_ingredients ORDER BY recipe_id, sort_order");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecipeBuilder b = builders.get(rs.getLong("recipe_id"));
                    if (b == null) continue;
                    // マスタに無い食材名は OTHER で自動追加(database.csv にも書き込む)
                    Ingredient ing = ingredientMaster.addIngredient(rs.getString("ingredient_name"));
                    if (ing == null) continue;
                    b.ingredients.add(ing);
                    b.quantities.put(ing.getName(), Quantity.of(rs.getString("amount"), rs.getString("unit")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("レシピの読み込みに失敗: " + e.getMessage(), e);
        }

        List<Recipe> result = new ArrayList<>();
        for (RecipeBuilder b : builders.values()) {
            // カテゴリーが空の場合は Recipe 側で OTHER になる
            Recipe r = new Recipe(b.title, b.url, b.ingredients, b.quantities, b.categories);
            r.setId(b.id);
            result.add(r);
        }
        return result;
    }

    /**
     * レシピを新規挿入し、生成された id を返すメソッド
     * @param recipe 保存するレシピ(id は無視される)
     * @return 自動採番された id
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

            insertChildren(conn, id, recipe);
            conn.commit();
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("レシピの保存に失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 既存レシピを更新するメソッド
     * @param recipe 同じ id を持つ更新後のレシピ
     * @throws IllegalArgumentException recipe.getId()が0以下のとき
     */
    public void update(Recipe recipe) {
        long id = recipe.getId();
        if (id <= 0) {
            throw new IllegalArgumentException("Recipe has no id; use insert() instead");
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

            // カテゴリーと食材は一旦消してから入れ直す
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_categories WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM recipe_ingredients WHERE recipe_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            insertChildren(conn, id, recipe);

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("レシピ更新失敗: " + e.getMessage(), e);
        }
    }

    /**
     * レシピのカテゴリーと食材を子テーブルへ挿入するメソッド(insert / update 共通)
     * @param conn   トランザクション中の接続
     * @param id     親レシピのDBキー
     * @param recipe 保存するレシピ
     * @throws SQLException 挿入失敗時
     */
    private void insertChildren(Connection conn, long id, Recipe recipe) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO recipe_categories(recipe_id, category) VALUES (?, ?)")) {
            for (RecipeCategory cat : recipe.getCategories()) {
                ps.setLong(1, id);
                ps.setString(2, cat.name());
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO recipe_ingredients(recipe_id, ingredient_name, sort_order, amount, unit) VALUES (?, ?, ?, ?, ?)")) {
            int order = 0;
            for (Ingredient ing : recipe.getIngredients()) {
                Quantity q = recipe.getQuantity(ing);
                ps.setLong(1, id);
                ps.setString(2, ing.getName());
                ps.setInt(3, order++);
                ps.setString(4, q.getAmount());
                ps.setString(5, q.getUnit());
                ps.executeUpdate();
            }
        }
    }

    /**
     * 指定 id のレシピを削除するメソッド
     * recipe_categories / recipe_ingredients の行は ON DELETE CASCADE で一緒に削除される
     * @param id 削除対象レシピのDBキー
     */
    public void delete(long id) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM recipes WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("レシピの削除に失敗: " + e.getMessage(), e);
        }
    }

    /** 読み込み中の中間状態を保持する内部Builder */
    private static class RecipeBuilder {
        final long id;
        final String title;
        final String url;
        final ArrayList<Ingredient> ingredients = new ArrayList<>();
        final Map<String, Quantity> quantities = new LinkedHashMap<>();
        final EnumSet<RecipeCategory> categories = EnumSet.noneOf(RecipeCategory.class);

        /**
         * @param id    DBのIDキー
         * @param title レシピ名
         * @param url   参照URL
         */
        RecipeBuilder(long id, String title, String url) {
            this.id = id;
            this.title = title;
            this.url = url;
        }
    }
}
