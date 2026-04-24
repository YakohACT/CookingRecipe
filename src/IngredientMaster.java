import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * CSVファイルから食材データを読み込み、プログラム全体で使えるように管理するクラス
 */
public class IngredientMaster {
    /** 食材オブジェクトをキー、食材名を値とするデータベース */
    private HashMap<Ingredient, String> database;

    /**
     * コンストラクタです。初期化時にCSVファイルからのデータ読み込みを実行する
     */
    public IngredientMaster() {
        database = new HashMap<>();
        loadCsv();
    }

    /**
     * database.csv から食材を読み取り、HashMapに登録する内部メソッド
     * ファイルの存在チェック、文字コードの指定、BOMの除去、スペルミスの自動修正を行う
     */
    private void loadCsv() {
        File file = new File("database.csv");
        if (!file.exists()) {
            System.out.println("【エラー】'database.csv' が見つかりません。プログラムと同じフォルダに配置してください。");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            int lineNumber = 0;
            int successCount = 0;
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.replace("\uFEFF", "").trim();
                
                if (line.isEmpty()) continue;

                String[] data = line.split(",");
                if (data.length >= 2) {
                    try {
                        String categoryStr = data[0].trim();
                        if (categoryStr.equals("CARBOHDRATE")) {
                            categoryStr = "CARBOHYDRATE";
                        }
                        
                        IngredientCategory cat = IngredientCategory.valueOf(categoryStr);
                        String name = data[1].trim();
                        Ingredient ingredient = new Ingredient(name, cat);
                        database.put(ingredient, name);
                        successCount++;
                    } catch (IllegalArgumentException e) {
                        System.out.println(lineNumber + "行目: カテゴリ「" + data[0] + "」が不正なためスキップしました。");
                    }
                }
            }
            System.out.println("【情報】database.csv から " + successCount + " 件の食材を読み込みました！");
            
        } catch (Exception e) {
            System.out.println("【エラー】database.csv の読み込み中に問題が発生しました。");
            System.out.println("原因: " + e.getMessage());
        }
    }

    /**
     * 読み込んだすべての食材データを保持するHashMapを取得する
     * * @return 食材データのHashMap
     */
    public HashMap<Ingredient, String> getDatabase() {
        return database;
    }

    /**
     * 指定されたカテゴリに属する食材をすべて取り出してリストにして返す
     * * @param category 検索したい食材のカテゴリ
     * @return 指定されたカテゴリに一致する食材のリスト
     */
    public ArrayList<Ingredient> searchIngredient(IngredientCategory category) {
        ArrayList<Ingredient> ingredientList = new ArrayList<>();
        for (Ingredient ingredient : database.keySet()) {
            if (ingredient.getCategory() == category) {
                ingredientList.add(ingredient);
            }
        }
        return ingredientList;
    }

    /**
     * データベースに登録されているすべての食材をリストとして取得する
     * * @return すべての食材のリスト
     */
    public ArrayList<Ingredient> getAllIngredients() {
        return new ArrayList<>(database.keySet());
    }
}