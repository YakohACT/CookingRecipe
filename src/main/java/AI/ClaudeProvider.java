package main.java.AI;

import main.java.Recipe.Ingredient;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Anthropic Claude API との通信を担当するクラス。
 * プロンプト側でJSON出力を強制している(API側にJSONモードはないため)。
 */
public class ClaudeProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        String safePrompt = jsonEscape(prompt);
        String json = "{"
                + "\"model\":\"" + modelName + "\","
                + "\"max_tokens\":1024,"
                + "\"temperature\":0.3,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}]"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("Claude API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        // content[0].text からモデルが返したJSON文字列を抽出してパース
        return extractAndParse(readStream(conn.getInputStream()), "text");
    }

    /**
     * /v1/models へGETしてAnthropicが提供する claude-* モデルを動的取得する。
     * 失敗時は静的フォールバック ({@link #getAvailableModels()}) を返す。
     * @param apiKey Claude APIキー
     * @return claude-* モデル一覧
     */
    @Override
    public String[] fetchAvailableModels(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return getAvailableModels();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.anthropic.com/v1/models").openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return getAvailableModels();

            String body = readStream(conn.getInputStream());
            java.util.List<String> ids = extractAllJsonStrings(body, "id");
            java.util.List<String> result = new java.util.ArrayList<>();
            for (String id : ids) {
                if (id.startsWith("claude-") && !result.contains(id)) result.add(id);
            }
            // 新しいモデルが上に来るよう逆順ソート
            result.sort(java.util.Comparator.reverseOrder());
            return result.isEmpty() ? getAvailableModels() : result.toArray(new String[0]);
        } catch (Exception e) {
            return getAvailableModels();
        }
    }

    @Override
    public String[] getAvailableModels() {
        // 2026年4月時点のアクティブモデル (claude-sonnet-4 / claude-opus-4 は2026/4/20退役済み)
        return new String[]{
                "claude-opus-4-7",          // 最高性能・最新フラッグシップ
                "claude-sonnet-4-6",        // 速度と性能のバランス
                "claude-haiku-4-5-20251001" // 最高速・低コスト
        };
    }
}
