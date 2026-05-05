package main.java.AI.Ollama;

import main.java.AI.AbstractRecipeAIProvider;
import main.java.Recipe.Ingredient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * ローカルLLM Ollama (http://localhost:11434/api/chat) との通信を担う抽象基底クラス。
 * 各モデルファミリ(Llama / Gemma / Mistral / Phi / Qwen) は本クラスを継承し、
 * getAvailableModels() で対応モデル名を返す。
 *
 * Ollama 共通の挙動:
 * - format: "json" を指定して必ずJSONで返させる
 * - temperature: 0.3 で安定化
 * - apiKey は不要(渡されても無視)
 */
public abstract class OllamaProvider extends AbstractRecipeAIProvider {

    /** Ollama サーバーのデフォルトURL */
    protected static final String OLLAMA_URL = "http://localhost:11434/api/chat";

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        String safePrompt = jsonEscape(prompt);

        String json = "{"
                + "\"model\":\"" + modelName + "\","
                + "\"stream\":false,"
                + "\"format\":\"json\","
                + "\"options\":{\"temperature\":0.3},"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}]"
                + "}";

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URL(OLLAMA_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(120000); // ローカルLLMは生成が遅いため長めに
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (ConnectException e) {
            throw new Exception("Ollama に接続できません。`ollama serve` が " + OLLAMA_URL
                    + " で起動しているか確認してください。", e);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("Ollama API エラー (HTTP " + responseCode + "): " + errorBody
                    + "\n(モデル '" + modelName + "' が `ollama pull` 済みかも確認してください)");
        }

        String jsonRes = readStream(conn.getInputStream());

        // Ollama レスポンス形式: {"message": {"role":"assistant","content":"..."}, "done": true, ...}
        // content フィールドの中身がモデルの返したJSON文字列
        int keyIdx = jsonRes.indexOf("\"content\":");
        if (keyIdx == -1) {
            throw new Exception("レスポンス解析失敗: contentフィールドが見つかりません。レスポンス: " + jsonRes);
        }
        int valueQuote = jsonRes.indexOf("\"", keyIdx + 10);
        if (valueQuote == -1) {
            throw new Exception("レスポンス解析失敗: contentフィールドの値が見つかりません");
        }
        int start = valueQuote + 1;
        int end = findUnescapedQuote(jsonRes, start);
        if (end == -1) throw new Exception("レスポンス解析失敗: contentフィールドの終端が見つかりません");

        return parseJsonResponse(jsonRes.substring(start, end));
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
