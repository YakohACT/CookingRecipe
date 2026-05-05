package main.java.AI;

import main.java.Recipe.Ingredient;

import java.io.*;
import java.net.*;
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
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("Claude API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        String jsonRes = readStream(conn.getInputStream());

        // content[0].text からJSON文字列を抜き出す（コロン後の空白有無に対応）
        int keyIdx = jsonRes.indexOf("\"text\":");
        if (keyIdx == -1) {
            throw new Exception("レスポンス解析失敗: textフィールドが見つかりません。レスポンス: " + jsonRes);
        }
        int valueQuote = jsonRes.indexOf("\"", keyIdx + 7);
        if (valueQuote == -1) {
            throw new Exception("レスポンス解析失敗: textフィールドの値が見つかりません。レスポンス: " + jsonRes);
        }
        int start = valueQuote + 1;
        int end = findUnescapedQuote(jsonRes, start);
        if (end == -1) throw new Exception("レスポンス解析失敗: textフィールドの終端が見つかりません");

        return parseJsonResponse(jsonRes.substring(start, end));
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
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
