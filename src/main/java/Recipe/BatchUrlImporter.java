package main.java.Recipe;

import main.java.AI.RecipeAIService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * URLを1行ずつ列挙したテキストファイルを読み込み、各URLをAIに解析するクラス
 *
 * - 空行または先頭が#の行は無視
 * - 既にDBに登録済みのURLはスキップ
 * - 1件ずつ順次AI呼び出し
 * - タイトル/食材どちらも検出できなかったURLはスキップ
 */
public final class BatchUrlImporter {

    /** 処理状態の列挙型 */
    public enum Status {
        /** AIへ問い合わせ中 */
        PROCESSING,
        /** 登録成功 */
        SUCCEEDED,
        /** AI例外処理 */
        FAILED,
        /** URL重複 */
        SKIPPED_DUPLICATE
    }

    /** バッチ処理結果を返すメソッド */
    public static final class Result {
        /**成功数*/
        public final int succeeded;
        /**失敗数*/
        public final int failed;
        /**重複数*/
        public final int skipped;
        /**URL総数(ファイル内重複を除く)*/
        public final int total;

        public Result(int succeeded, int failed, int skipped, int total) {
            this.succeeded = succeeded;
            this.failed = failed;
            this.skipped = skipped;
            this.total = total;
        }
    }

    /** 進捗通知を行うメソッド */
    public static final class Progress {
        /** 処理中インデックス */
        public final int index;
        /** 全体URL数 */
        public final int total;
        /** 現在処理中の URL */
        public final String url;
        /** 1件あたりの状態 */
        public final Status status;

        public Progress(int index, int total, String url, Status status) {
            this.index = index;
            this.total = total;
            this.url = url;
            this.status = status;
        }
    }

    private BatchUrlImporter() {}

    /**
     * ファイルからURLを読み込み、空行・コメント(#始まり)・重複を排除して返すメソッド
     * @param file 入力ファイル
     * @return 重複排除済みURLリスト
     * @throws IOException 例外処理
     */
    public static List<String> readUrls(Path file) throws IOException {
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.replace("﻿", "").trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            uniq.add(trimmed);
        }
        return new ArrayList<>(uniq);
    }

    /**
     * URLリストを順次処理してレシピとして登録するメソッド
     * 既に DB に同じ URL のレシピが登録されている場合はスキップする
     * @param urls         処理対象URLリスト
     * @param aiService    設定済みのAIサービス
     * @param master       食材マスタ
     * @param recipeList   登録先のレシピリスト
     * @param onProgress   進捗通知コールバック
     * @param cancelCheck  キャンセル要求の問い合わせ
     * @return 集計結果
     */
    public static Result importAll(List<String> urls,
                                   RecipeAIService aiService,
                                   IngredientMaster master,
                                   AllRecipeList recipeList,
                                   Consumer<Progress> onProgress,
                                   BooleanSupplier cancelCheck) {
        // 開始時点で DB に登録済みの URL を集めておき、AI を呼び出さずにスキップ判定
        Set<String> existingUrls = new HashSet<>();
        for (Recipe r : recipeList.getRecipeList()) {
            if (r.getUrl() != null && !r.getUrl().isEmpty()) existingUrls.add(r.getUrl());
        }

        int succeeded = 0;
        int failed = 0;
        int skipped = 0;

        for (int i = 0; i < urls.size(); i++) {
            if (cancelCheck != null && cancelCheck.getAsBoolean()) break;
            String url = urls.get(i);

            //URL重複チェック
            if (existingUrls.contains(url)) {
                skipped++;
                System.out.println("[BatchUrlImporter] 既登録のためスキップ: " + url);
                if (onProgress != null) {
                    onProgress.accept(new Progress(i, urls.size(), url, Status.SKIPPED_DUPLICATE));
                }
                continue;
            }

            // 処理中通知
            if (onProgress != null) {
                onProgress.accept(new Progress(i, urls.size(), url, Status.PROCESSING));
            }

            Status finalStatus = Status.FAILED;
            try {
                String[] result = aiService.suggestRecipe(url, master.getAllIngredientNames());
                Recipe recipe = buildRecipe(result, url, master);
                if (recipe != null) {
                    recipeList.addRecipe(recipe);
                    existingUrls.add(url); // 後続でこのURLが再出現しても二重登録されないよう即時反映
                    finalStatus = Status.SUCCEEDED;
                } else {
                    System.err.println("[BatchUrlImporter] レシピ検出失敗(タイトルor食材0件): " + url);
                }
            } catch (Exception ex) {
                System.err.println("[BatchUrlImporter] " + url + " => " + ex.getMessage());
            }

            if (finalStatus == Status.SUCCEEDED) succeeded++;
            else failed++;

            // 結果通知(UI側で進捗バーやステータス表示の更新に利用)
            if (onProgress != null) {
                onProgress.accept(new Progress(i, urls.size(), url, finalStatus));
            }
        }
        return new Result(succeeded, failed, skipped, urls.size());
    }

    /**
     * AIからの回答を処理するメソッド
     * @param result AIからの戻り値
     * @param url    元のURL
     * @param master 食材マスタ
     * @return 構築された Recipe。検出失敗時は null
     */
    private static Recipe buildRecipe(String[] result, String url, IngredientMaster master) {
        String title = result[0].trim();
        // 未知の食材は OTHER カテゴリで自動的にマスタに追加される
        ArrayList<Ingredient> matched = master.resolveAll(result[1]);

        if (title.isEmpty() || matched.isEmpty()) return null;
        return new Recipe(title, url, matched, EnumSet.of(RecipeCategory.OTHER));
    }
}
