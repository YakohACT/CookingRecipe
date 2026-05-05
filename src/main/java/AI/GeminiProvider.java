package main.java.AI;

import main.java.Recipe.Ingredient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Google Gemini との通信を担当するクラス。
 * responseSchema による構造化JSON出力を強制することで、
 * 食材リストから外れたレシピや余計な装飾文を返さないようにしている。
 */
public class GeminiProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String cleanKey = apiKey.trim();
        String prompt = buildPrompt(url, allIngredients);
        String safePrompt = jsonEscape(prompt);

        StringBuilder partsJson = new StringBuilder();
        partsJson.append("{\"text\":\"").append(safePrompt).append("\"}");

        // YouTube URL の場合のみ動画自体を入力に添付する。
        // mime_type を明示しないとGeminiがURLを汎用Web取得として扱い、
        // YouTube が返す text/html を「Unsupported MIME type」として弾くため必須。
        // また list= や index= 等の余計なクエリが付いていると INVALID_ARGUMENT になるので、
        // 動画IDだけ抜き出して canonical な watch?v=... URL に正規化する。
        if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
            String canonical = canonicalYoutubeUrl(url.trim());
            String safeUrl = jsonEscape(canonical);
            partsJson.append(",{\"file_data\":{\"mime_type\":\"video/mp4\",\"file_uri\":\"")
                     .append(safeUrl).append("\"}}");
        }

        // 構造化出力で {title, ingredients[]} のJSONを必ず返させ、温度を下げて安定化する
        String generationConfig = "\"generationConfig\":{"
                + "\"responseMimeType\":\"application/json\","
                + "\"responseSchema\":{"
                +   "\"type\":\"OBJECT\","
                +   "\"properties\":{"
                +     "\"title\":{\"type\":\"STRING\"},"
                +     "\"ingredients\":{\"type\":\"ARRAY\",\"items\":{\"type\":\"STRING\"}}"
                +   "},"
                +   "\"required\":[\"title\",\"ingredients\"]"
                + "},"
                + "\"temperature\":0.3"
                + "}";

        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName
                + ":generateContent?key=" + cleanKey;
        String json = "{\"contents\":[{\"parts\":[" + partsJson + "]}]," + generationConfig + "}";

        String jsonRes = postWithRetry(urlStr, json);

        // candidates[0].content.parts[0].text からJSON文字列を抜き出す
        int keyIdx = jsonRes.indexOf("\"text\":");
        if (keyIdx == -1) throw new Exception("レスポンス解析失敗: textフィールドが見つかりません。レスポンス: " + jsonRes);
        int valueQuote = jsonRes.indexOf("\"", keyIdx + 7);
        if (valueQuote == -1) throw new Exception("レスポンス解析失敗: textフィールドの値が見つかりません。レスポンス: " + jsonRes);
        int start = valueQuote + 1;
        int end = findUnescapedQuote(jsonRes, start);
        if (end == -1) throw new Exception("レスポンス解析失敗: textフィールドの終端が見つかりません");

        return parseJsonResponse(jsonRes.substring(start, end));
    }

    /**
     * 503 (一時的サーバー過負荷) の場合は指数バックオフでリトライする
     */
    private String postWithRetry(String urlStr, String json) throws Exception {
        int maxRetries = 3;
        int delayMs = 2000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readStream(conn.getInputStream());
            }
            String errorBody = readStream(conn.getErrorStream());
            if (responseCode == 503 && attempt < maxRetries) {
                Thread.sleep(delayMs);
                delayMs *= 2;
                continue;
            }
            throw new Exception("Gemini API エラー (HTTP " + responseCode + "): " + errorBody);
        }
        throw new Exception("Gemini API エラー: リトライ上限に到達しました");
    }

    /**
     * YouTube URL から動画IDだけ抜き出して canonical な watch?v=... に変換する。
     * playlist や index などの余計なクエリ、Shorts/embed/youtu.be 形式に対応。
     * 解析できない場合は原文をそのまま返す
     */
    static String canonicalYoutubeUrl(String url) {
        String videoId = extractYoutubeVideoId(url);
        if (videoId == null) return url;
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private static String extractYoutubeVideoId(String url) {
        // youtu.be/<id>
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "youtu\\.be/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        // youtube.com/shorts/<id>
        m = java.util.regex.Pattern.compile(
                "youtube\\.com/shorts/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        // youtube.com/embed/<id>
        m = java.util.regex.Pattern.compile(
                "youtube\\.com/embed/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        // ?v=<id> or &v=<id>
        m = java.util.regex.Pattern.compile(
                "[?&]v=([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    @Override
    public String[] getAvailableModels() {
        // 2026年4月時点のアクティブモデル
        // Gemini 1.x / 2.0 系は全て終了(404エラー)のため除外
        return new String[]{
                "gemini-3.1-pro-preview",  // 最高性能・最新
                "gemini-2.5-pro",          // 高性能・安定版
                "gemini-2.5-flash",        // 高速・高性能バランス
                "gemini-2.5-flash-lite"    // 最速・低コスト
        };
    }
}
