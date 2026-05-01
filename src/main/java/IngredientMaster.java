import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * CSVから食材データを読み込み管理するクラス
 */
public class IngredientMaster {
    private HashMap<Ingredient, String> database;
    public IngredientMaster() {
        database = new HashMap<>();
        loadCsv();
    }

    /**
     * database.csv からのデータ読み込み
     * 実行ディレクトリの違いに対応するため複数候補を試行する
     */
    private void loadCsv() {
        String[] candidates = {
                "database.csv",
                "CookingRecipe/database.csv",
                "../database.csv",
                "../../database.csv",
                "../../../database.csv"
        };
        File file = null;
        for (String path : candidates) {
            File candidate = new File(path);
            if (candidate.exists()) {
                file = candidate;
                break;
            }
        }
        if (file == null) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 2) {
                    try {
                        String catStr = data[0].trim();
                        IngredientCategory cat = IngredientCategory.valueOf(catStr);
                        String name = data[1].trim();
                        database.put(new Ingredient(name, cat), name);
                    } catch (Exception e) {}
                }
            }
        } catch (Exception e) {}
    }

    public ArrayList<Ingredient> searchIngredient(IngredientCategory category) {
        ArrayList<Ingredient> list = new ArrayList<>();
        for (Ingredient ing : database.keySet()) {
            if (ing.getCategory() == category) list.add(ing);
        }
        return list;
    }

    public ArrayList<Ingredient> getAllIngredients() {
        return new ArrayList<>(database.keySet());
    }
}