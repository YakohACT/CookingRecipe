package main.java.AI.Ollama;

import main.java.AI.AbstractRecipeAIProvider;
import main.java.Recipe.Ingredient;

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
    /** Ollama インストール済みモデル一覧API */
    protected static final String OLLAMA_TAGS_URL = "http://localhost:11434/api/tags";

    /**
     * ローカルOllamaに `GET /api/tags` を送って実際にpull済みのモデル名を取得する。
     * Ollamaが起動していない/失敗時はサブクラスの静的リスト({@link #getAvailableModels()})にフォールバック。
     * @param apiKey 未使用(API互換のため受け取るが無視する)
     * @return Ollamaに導入済みのモデル名配列
     */
    @Override
    public String[] fetchAvailableModels(String apiKey) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(OLLAMA_TAGS_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return getAvailableModels();
            String body = readStream(conn.getInputStream());
            // {"models":[{"name":"llama3:8b","modified_at":"...",...}, ...]}
            java.util.List<String> names = extractAllJsonStrings(body, "name");
            java.util.List<String> uniq = new java.util.ArrayList<>();
            for (String n : names) {
                if (n != null && !n.isEmpty() && !uniq.contains(n)) uniq.add(n);
            }
            java.util.Collections.sort(uniq);
            return uniq.isEmpty() ? getAvailableModels() : uniq.toArray(new String[0]);
        } catch (Exception e) {
            return getAvailableModels();
        }
    }

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

        // Ollama レスポンス形式: {"message": {"role":"assistant","content":"..."}, "done": true, ...}
        // content フィールドの中身がモデルの返したJSON文字列
        return extractAndParse(readStream(conn.getInputStream()), "content");
    }
}
