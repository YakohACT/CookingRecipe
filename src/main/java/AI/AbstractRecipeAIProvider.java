package main.java.AI;

import main.java.Util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 各AIプロバイダーの共通処理をまとめた抽象クラス
 */
public abstract class AbstractRecipeAIProvider {

    /**
     * 任意のプロンプトをモデルへ送り、モデルが返した本文(テキスト)をそのまま返す汎用呼び出し
     * @param apiKey    APIキー(ローカルLLMは未使用)
     * @param modelName モデル名
     * @param prompt    送信するプロンプト本文
     * @return モデルが返したテキスト(unescape済み。多くの場合JSON文字列)
     * @throws Exception API呼び出し/解析失敗時
     */
    public abstract String chat(String apiKey, String modelName, String prompt) throws Exception;

    /**
     * このプロバイダーで選択可能なモデル名の配列を返す
     * @return モデル名配列
     */
    public abstract String[] getAvailableModels();

    /**
     * URLを参考にレシピ案(料理名と食材)を生成する
     * レシピ用プロンプトを {@link #chat} で送り、返ったJSONを解析する。
     * 動画の添付など独自のリクエストが必要なプロバイダーはオーバーライドする
     * @param apiKey         APIキー(ローカルLLMは未使用)
     * @param modelName      モデル名
     * @param url             レシピ参照URL
     * @param ingredientNames 利用可能な食材名のリスト
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     * @throws Exception API呼び出し/解析失敗時
     */
    public String[] generateRecipe(String apiKey, String modelName, String url, List<String> ingredientNames) throws Exception {
        return parseRecipeJson(chat(apiKey, modelName, buildPrompt(url, ingredientNames)));
    }

    /**
     * APIキーを使って動的にモデル一覧を取得する。
     * デフォルト実装は {@link #getAvailableModels()} の静的フォールバックを返す
     * 各プロバイダーは必要に応じてオーバーライドしてHTTPリストAPIを叩く
     * @param apiKey APIキー(ローカルLLMは未使用)
     * @return 動的取得できたモデル名配列。失敗時はフォールバック
     */
    public String[] fetchAvailableModels(String apiKey) {
        return getAvailableModels();
    }

    /**
     * 食材候補からレシピを提案させるプロンプトを構築する
     * 候補リストを先頭に置き、JSON形式での回答を強制
     * @param url             参照URL
     * @param ingredientNames 利用可能な食材名のリスト
     * @return AIへ送信するプロンプト文字列
     */
    protected String buildPrompt(String url, List<String> ingredientNames) {
        String names = String.join(", ", ingredientNames);

        boolean isYoutube = YoutubeMetadataFetcher.isYoutubeUrl(url);
        // 参考情報の取得:
        //   - YouTube URL → oEmbedで動画タイトル + watchページから字幕を取得
        //   - YouTube以外 → HTMLを取得してテキスト化(WebPageFetcher)
        String pageText = "";
        if (!url.trim().isEmpty()) {
            if (isYoutube) {
                // 概要欄に材料が記載されているか判定するため食材名リストを渡す
                pageText = YoutubeMetadataFetcher.fetchSummary(url, ingredientNames);
            } else {
                pageText = WebPageFetcher.fetchSummary(url);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("あなたは日本の家庭料理のシェフです。\n\n");
        sb.append("【利用可能な食材】\n");
        sb.append(names).append("\n\n");
        if (!pageText.isEmpty()) {
            sb.append(isYoutube ? "【参考動画(指定URLから取得)】\n" : "【参考レシピページ(指定URLから取得)】\n");
            sb.append(pageText).append("\n\n");
        }
        sb.append("【ルール】\n");
        sb.append("・食材は【利用可能な食材】リストから優先して選ぶこと。同じ役割の食材がリストにあれば、必ずそちらを使うこと(例: リストに「醤油」があるのに「だし醤油」と書かない)\n");
        sb.append("・リスト内の食材だけで成立しないレシピのときに限り、リスト外の食材名を使ってよい(その場合も最小限に留める)\n");
        sb.append("・食材名は【利用可能な食材】リストの表記をそのまま使うこと(例: リストの「鶏もも」を「鶏もも肉」と書き換えない)\n");
        sb.append("・前置き・補足・Markdown装飾・コードフェンスは一切禁止\n");
        if (isYoutube) {
            if (!pageText.isEmpty()) {
                sb.append("・上記の動画タイトル/字幕の料理を再現するように料理名と食材を選ぶこと\n");
            } else {
                sb.append("・動画の内容を参考にすること\n");
            }
        } else if (!pageText.isEmpty()) {
            sb.append("・参考レシピページの料理を再現するように料理名と食材を選ぶこと\n");
        }
        sb.append("\n【出力形式】\n");
        sb.append("以下のキーを持つJSONオブジェクトのみを返してください。\n");
        sb.append("- title: 料理名 (文字列)\n");
        sb.append("- ingredients: 食材名の配列 (文字列の配列)。原則【利用可能な食材】リストの表記から選ぶこと\n");
        return sb.toString();
    }

    /**
     * モデルが返したレシピJSON({title, ingredients[]})を解読し、
     * {タイトル, 食材(カンマ連結)} の形に変換する
     * @param text モデル本文(unescape済みのJSON文字列)
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     */
    protected String[] parseRecipeJson(String text) {
        // フォールバックを空文字にすることで、UI側で「AIがレシピを検出できなかった」状態を判定できるようにする
        String title = JsonUtil.extractString(text, "title");

        StringBuilder joined = new StringBuilder();
        for (String ing : JsonUtil.extractStringArray(text, "ingredients")) {
            String cleaned = stripBrackets(ing);
            if (cleaned.isEmpty()) continue;
            if (joined.length() > 0) joined.append(",");
            joined.append(cleaned);
        }
        return new String[]{title == null ? "" : stripBrackets(title), joined.toString()};
    }

    /**
     * AIがプレースホルダや装飾を含めた場合のために角括弧・カギカッコ・先頭の箇条書き記号などを除去
     * @param s 元の文字列
     * @return 装飾を除去した文字列
     */
    private String stripBrackets(String s) {
        return s.replaceAll("[\\[\\]【】「」]", "")
                .replaceAll("^[\\s\\-*\\u30FB・]+", "")
                .trim();
    }

    /**
     * InputStream を UTF-8 文字列として読み切る
     * 各プロバイダーが個別実装していたものを共通化したヘルパー
     * @param stream 入力ストリーム(null可)
     * @return ストリーム内容の文字列(null入力時は空文字)
     * @throws IOException 読み込み失敗時
     */
    protected static String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /**
     * JSONボディをPOSTする接続を開き、ヘッダー設定とボディ送信まで済ませる。
     * レスポンスの読み取りは {@link #readOk} で行う。
     * @param url         送信先URL
     * @param body        リクエストボディJSON
     * @param connectMs   接続タイムアウト(ミリ秒)
     * @param readMs      読み取りタイムアウト(ミリ秒)
     * @param headerPairs 追加ヘッダーを「名前, 値, 名前, 値, …」の順で並べたもの
     * @return ボディ送信済みの接続
     * @throws IOException 接続/送信失敗時
     */
    protected static HttpURLConnection openJsonPost(String url, String body, int connectMs, int readMs,
                                                    String... headerPairs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        for (int i = 0; i + 1 < headerPairs.length; i += 2) {
            conn.setRequestProperty(headerPairs[i], headerPairs[i + 1]);
        }
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    /**
     * HTTP 200 ならレスポンスボディを返し、それ以外はエラーボディを含めた例外を投げる。
     * @param conn       リクエスト送信済みの接続
     * @param errorLabel 例外メッセージの先頭(例: "OpenAI API エラー")
     * @param hint       例外メッセージの末尾に付ける補足(不要なら空文字)
     * @return レスポンスボディ文字列
     * @throws Exception HTTP非200時
     */
    protected static String readOk(HttpURLConnection conn, String errorLabel, String hint) throws Exception {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception(errorLabel + " (HTTP " + responseCode + "): " + errorBody + hint);
        }
        return readStream(conn.getInputStream());
    }

    /**
     * GETリクエストを送り、HTTP 200 ならレスポンスボディを返す。
     * モデル一覧の取得など、失敗してもフォールバックすればよい用途向け。
     * @param url         取得先URL
     * @param connectMs   接続タイムアウト(ミリ秒)
     * @param readMs      読み取りタイムアウト(ミリ秒)
     * @param headerPairs 追加ヘッダーを「名前, 値, 名前, 値, …」の順で並べたもの
     * @return レスポンスボディ。HTTP非200や通信失敗時は null
     */
    protected static String httpGetOrNull(String url, int connectMs, int readMs, String... headerPairs) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            for (int i = 0; i + 1 < headerPairs.length; i += 2) {
                conn.setRequestProperty(headerPairs[i], headerPairs[i + 1]);
            }
            conn.setConnectTimeout(connectMs);
            conn.setReadTimeout(readMs);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            return readStream(conn.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * APIレスポンス全体から、モデル本文が入っている指定キーの文字列値を取り出す
     * OpenAI/Ollama/llama.cpp は "content"、Claude/Gemini は "text" フィールドに
     * モデルが返したテキストがエスケープされた文字列として入っている
     * @param jsonRes    APIレスポンス全体のJSON文字列
     * @param contentKey 抜き出す対象キー名(例: "content", "text")
     * @return モデル本文(unescape済み)
     * @throws Exception キーが見つからない/値が文字列でない場合
     */
    protected String extractContent(String jsonRes, String contentKey) throws Exception {
        String content = JsonUtil.extractString(jsonRes, contentKey);
        if (content == null) {
            throw new Exception("レスポンス解析失敗: " + contentKey + "フィールドが見つかりません。レスポンス: " + jsonRes);
        }
        return content;
    }
}
