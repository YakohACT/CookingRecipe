package main.java.Recipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * CSVから食材データを読み込み管理するクラス。
 * LinkedHashMap を使うことで、CSVに記述された順序のまま取り出せる
 */
public class IngredientMaster {
    private LinkedHashMap<Ingredient, String> database;

    /**
     * インスタンス生成と同時に database.csv を読み込む。
     * CSV が見つからない場合は database が空のまま生成される。
     */
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
     * @return 見つかった CSV ファイル。どこにも無ければ null
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

    /**
     * 指定カテゴリーに属する食材を CSV 順で返す。
     * @param category 絞り込みたい食材カテゴリー
     * @return 該当食材のリスト(マッチが無い場合は空のリスト)
     */
    public ArrayList<Ingredient> searchIngredient(IngredientCategory category) {
        ArrayList<Ingredient> list = new ArrayList<>();
        for (Ingredient ing : database.keySet()) {
            if (ing.getCategory() == category) list.add(ing);
        }
        return list;
    }

    /**
     * 全食材を CSV に記述された順序で返す。
     * @return 全食材のリスト
     */
    public ArrayList<Ingredient> getAllIngredients() {
        return new ArrayList<>(database.keySet());
    }

    /**
     * 食材一覧を一括置換する(順序・カテゴリの変更を反映)。
     * メモリ上の database を引数のリストの内容で書き換え、database.csv 全体を書き直す。
     * 順序はリストの順序がそのまま使われる。
     * @param ordered 新しい食材一覧(順序を保持)
     */
    public void replaceAll(List<Ingredient> ordered) {
        if (ordered == null) return;
        database.clear();
        for (Ingredient ing : ordered) {
            if (ing == null || ing.getName() == null || ing.getName().isEmpty()) continue;
            database.put(ing, ing.getName());
        }
        saveCsv();
    }

    /**
     * 現在のメモリ上の食材一覧を database.csv に上書き保存する。
     */
    private void saveCsv() {
        File csv = locateCsv();
        if (csv == null) {
            csv = new File("database.csv");
        }
        try {
            List<String> lines = new ArrayList<>();
            for (Ingredient ing : database.keySet()) {
                lines.add(ing.getCategory().name() + "," + ing.getName());
            }
            Files.write(csv.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[IngredientMaster] CSV保存に失敗: " + e.getMessage());
        }
    }

    /**
     * 食材を OTHER カテゴリで追加する(同名の食材が既にあればそれを返す)。
     * メモリ上のマスタ・database.csv の両方に反映される。
     * @param name 食材名
     * @return 追加された(または既存の)Ingredient。空文字/null の場合は null
     */
    public Ingredient addIngredient(String name) {
        return addIngredient(name, IngredientCategory.OTHER);
    }

    /**
     * 食材を指定カテゴリで追加する(同名の食材が既にあればそれを返す)。
     * メモリ上のマスタ・database.csv の両方に反映される。
     * @param name     食材名
     * @param category 食材カテゴリ
     * @return 追加された(または既存の)Ingredient。空文字/null の場合は null
     */
    public Ingredient addIngredient(String name, IngredientCategory category) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;
        // 既に同名の食材があればそれを返す(equalsは名前一致)
        for (Ingredient existing : database.keySet()) {
            if (existing.getName().equals(trimmed)) return existing;
        }
        Ingredient newIng = new Ingredient(trimmed, category);
        database.put(newIng, trimmed);
        appendToCsv(category, trimmed);
        return newIng;
    }

    /**
     * database.csv の末尾に "<CATEGORY>,<name>" を追記する。
     * 既に同じ行があればスキップする。失敗時は黙殺する(マスタ追加自体は in-memory に反映済み)。
     * @param category 食材カテゴリ
     * @param name     食材名
     */
    private void appendToCsv(IngredientCategory category, String name) {
        File csv = locateCsv();
        if (csv == null) {
            // 既存のCSVが見つからない場合は実行カレントに新規作成する
            csv = new File("database.csv");
        }
        String entry = category.name() + "," + name;
        try {
            List<String> lines = csv.exists()
                    ? Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8)
                    : new ArrayList<>();
            // 既に同じ行がある場合は何もしない(冪等)。BOM混入も考慮
            for (String l : lines) {
                if (l.replace("﻿", "").trim().equals(entry)) return;
            }
            lines.add(entry);
            Files.write(csv.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            // 読み書き失敗は無視。in-memoryの追加は既に成功している
        }
    }
}