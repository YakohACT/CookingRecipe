package main.java.AI;

import main.java.Util.JsonUtil;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ローカルLLM Ollama (http://localhost:11434/api/chat) との通信を担当するクラス。
 * モデルファミリ(Llama / Gemma / Mistral / Phi / Qwen など)によるAPIの違いは無いため、
 * どのモデルもこのクラスで扱う。
 *
 * Ollama 共通の挙動:
 * - format: "json" を指定して必ずJSONで返させる
 * - temperature: 0.3 で安定化
 * - apiKey は不要(渡されても無視)
 */
public class OllamaProvider extends AbstractRecipeAIProvider {

    /** Ollama サーバーのデフォルトURL */
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    /** Ollama インストール済みモデル一覧API */
    private static final String OLLAMA_TAGS_URL = "http://localhost:11434/api/tags";

    /**
     * ローカルOllamaに `GET /api/tags` を送って実際にpull済みのモデル名を取得する。
     * Ollamaが起動していない/失敗時は静的リスト({@link #getAvailableModels()})にフォールバック。
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
            java.util.List<String> names = JsonUtil.extractAllStrings(body, "name");
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
    public String chat(String apiKey, String modelName, String prompt) throws Exception {
        // Ollama レスポンス形式: {"message": {"role":"assistant","content":"..."}, "done": true, ...}
        return extractContent(post(modelName, prompt), "content");
    }

    /**
     * Ollama /api/chat へPOSTし、レスポンスボディ全体を返す。
     * format:"json" で必ずJSONを返させ、温度を下げて安定化する。
     * @param modelName モデル名
     * @param prompt    ユーザープロンプト
     * @return レスポンスボディ文字列
     * @throws Exception 接続不可 / HTTP非200時
     */
    private String post(String modelName, String prompt) throws Exception {
        String json = "{"
                + "\"model\":\"" + modelName + "\","
                + "\"stream\":false,"
                + "\"format\":\"json\","
                + "\"options\":{\"temperature\":0.3},"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + JsonUtil.escape(prompt) + "\"}]"
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
        return readStream(conn.getInputStream());
    }

    /**
     * Ollama から導入済みモデルを取得できないときに表示する、動作確認済みモデルの一覧を返す。
     * @return 動作確認済みモデル名の配列
     */
    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "llama3:8b",
                "gemma4:e2b",
                "gemma4:e4b",
                "mistral",
                "mistral:7b",
                "mistral-nemo",
                "mistral-small",
                "mixtral",
                "phi4",
                "phi3.5",
                "phi3",
                "phi3:mini",
                "phi3:medium",
                "qwen3.5:4b",
                "qwen3.5:9b",
                "qwen3-vl:4b",
                "qwen3-vl:8b"
        };
    }
}
