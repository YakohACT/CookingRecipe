package main.java.AI;

import main.java.Recipe.Ingredient;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * OpenAI ChatCompletions API との通信を担当するクラス。
 * response_format=json_object でJSON出力を強制している。
 */
public class OpenAIProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        String safePrompt = jsonEscape(prompt);

        // response_format で JSON オブジェクト出力を強制し、温度を下げて安定化する
        String json = "{"
                + "\"model\":\"" + modelName + "\","
                + "\"response_format\":{\"type\":\"json_object\"},"
                + "\"temperature\":0.3,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}]"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("OpenAI API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        String jsonRes = readStream(conn.getInputStream());

        // choices[0].message.content からJSON文字列を抜き出す（コロン後の空白有無に対応）
        int keyIdx = jsonRes.indexOf("\"content\":");
        if (keyIdx == -1) {
            throw new Exception("レスポンス解析失敗: contentフィールドが見つかりません。レスポンス: " + jsonRes);
        }
        int valueQuote = jsonRes.indexOf("\"", keyIdx + 10);
        if (valueQuote == -1) {
            throw new Exception("レスポンス解析失敗: contentフィールドの値が見つかりません。レスポンス: " + jsonRes);
        }
        int start = valueQuote + 1;
        int end = findUnescapedQuote(jsonRes, start);
        if (end == -1) throw new Exception("レスポンス解析失敗: contentフィールドの終端が見つかりません");

        return parseJsonResponse(jsonRes.substring(start, end));
    }

    /**
     * InputStream を UTF-8 文字列として読み切る。
     * @param stream 入力ストリーム(null可)
     * @return ストリーム内容の文字列(null入力時は空文字)
     * @throws IOException 読み込み失敗時
     */
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /**
     * /v1/models へGETしてアカウントで利用可能なモデルIDを動的取得する。
     * チャット系(gpt-/o1/o3/o4/chatgpt-)のみフィルタし、音声・埋め込み等の非対象を除外する。
     * 失敗時は静的フォールバック ({@link #getAvailableModels()}) を返す。
     * @param apiKey OpenAI APIキー
     * @return 利用可能なチャットモデル一覧
     */
    @Override
    public String[] fetchAvailableModels(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return getAvailableModels();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/models").openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return getAvailableModels();

            String body = readStream(conn.getInputStream());
            java.util.List<String> ids = extractAllJsonStrings(body, "id");
            java.util.List<String> filtered = new java.util.ArrayList<>();
            for (String id : ids) {
                if (!isChatModel(id)) continue;
                filtered.add(id);
            }
            // モデル名で安定ソート
            java.util.Collections.sort(filtered);
            return filtered.isEmpty() ? getAvailableModels() : filtered.toArray(new String[0]);
        } catch (Exception e) {
            return getAvailableModels();
        }
    }

    /**
     * モデルIDがチャット用途かを判定する(/v1/chat/completions に投げて使えるもの)。
     * @param id OpenAIモデルID
     * @return チャット用途として妥当なら true
     */
    private static boolean isChatModel(String id) {
        if (id == null) return false;
        String s = id.toLowerCase();
        // チャット系プレフィックス
        boolean chatPrefix = s.startsWith("gpt-") || s.startsWith("o1") || s.startsWith("o3")
                || s.startsWith("o4") || s.startsWith("chatgpt-");
        if (!chatPrefix) return false;
        // 音声/埋め込み/画像/転写は除外
        return !s.contains("audio") && !s.contains("realtime") && !s.contains("transcribe")
                && !s.contains("tts") && !s.contains("embedding") && !s.contains("whisper")
                && !s.contains("dall-e") && !s.contains("image");
    }

    @Override
    public String[] getAvailableModels() {
        // 2026年4月時点で /v1/chat/completions エンドポイントに対応しているモデル
        return new String[]{
                "gpt-5.5",        // 最高性能フラッグシップ
                "gpt-5.4",        // 高性能・汎用
                "gpt-5.4-mini",   // 高速・低コスト
                "gpt-5.4-nano",   // 最速・最安
                "gpt-4.1",        // 安定版汎用
                "gpt-4.1-mini",   // 安定版・軽量
                "gpt-4.1-nano",   // 安定版・最軽量
                "gpt-4o",         // 旧世代・マルチモーダル
                "gpt-4o-mini"     // 旧世代・軽量
        };
    }
}
