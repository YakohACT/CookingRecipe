package main.java.Recipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * CSVから食材データを読み込み管理するクラス。
 * LinkedHashMap を使うことで、CSVに記述された順序のまま取り出せる
 */
public class IngredientMaster {
    private LinkedHashMap<Ingredient, String> database;
    public IngredientMaster() {
        database = new LinkedHashMap<>();
        loadCsv();
    }

    /**
     * database.csv からのデータ読み込み。
     * 実行ディレクトリ依存の相対パスをまず試し、それでも見つからない場合は
     * 自身のJAR(またはクラスファイル)と同階層・1つ上の階層も探す。
     * jpackage で配布された .exe からの起動でも見つかるようになっている
     */
    private void loadCsv() {
        File file = locateCsv();
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

    /**
     * database.csv をいくつかの場所から探す。
     * 1. 実行カレント (相対パス候補)
     * 2. 自身がロードされた JAR/クラスディレクトリと同階層
     * 3. その親ディレクトリ
     */
    private File locateCsv() {
        String[] cwdCandidates = {
                "database.csv",
                "CookingRecipe/database.csv",
                "../database.csv",
                "../../database.csv",
                "../../../database.csv"
        };
        for (String path : cwdCandidates) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        try {
            File codeSource = new File(IngredientMaster.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File baseDir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
            if (baseDir != null) {
                File sameDir = new File(baseDir, "database.csv");
                if (sameDir.exists()) return sameDir;
                File parentDir = baseDir.getParentFile();
                if (parentDir != null) {
                    File parentCsv = new File(parentDir, "database.csv");
                    if (parentCsv.exists()) return parentCsv;
                }
            }
        } catch (Exception ignore) {}
        return null;
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