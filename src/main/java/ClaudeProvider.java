import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClaudeProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        // ダブルクォートのみエスケープ（\n はすでにJSON用エスケープ済みのため二重エスケープしない）
        String safePrompt = prompt.replace("\"", "\\\"");
        String json = "{\"model\":\"" + modelName + "\",\"max_tokens\":1024,\"messages\":"
                + "[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        // HTTPエラー時はエラーストリームから詳細メッセージを取得
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("Claude API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        String jsonRes = readStream(conn.getInputStream());

        // "text": "..." の開始位置を特定
        int start = jsonRes.indexOf("\"text\": \"");
        if (start == -1) {
            throw new Exception("レスポンス解析失敗: textフィールドが見つかりません。レスポンス: " + jsonRes);
        }
        start += 9; // "text": " の文字数分進める

        // バックスラッシュエスケープを考慮して終端クォートを探す
        int end = findUnescapedQuote(jsonRes, start);

        return parseStandardResponse(jsonRes.substring(start, end));
    }

    /** ストリームを読み込んで文字列として返す。nullの場合は空文字を返す */
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** pos 以降でエスケープされていない最初の '"' の位置を返す */
    private int findUnescapedQuote(String s, int pos) throws Exception {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '\\') {
                pos += 2; // エスケープシーケンスをスキップ
                continue;
            }
            if (c == '"') return pos;
            pos++;
        }
        throw new Exception("レスポンス解析失敗: textフィールドの終端が見つかりません");
    }

    @Override
    public String[] getAvailableModels() {
        // 2026年4月時点のアクティブモデル (claude-sonnet-4 / claude-opus-4 は2026/4/20退役済み)
        return new String[]{
                "claude-opus-4-7",        // 最高性能・最新フラッグシップ
                "claude-sonnet-4-6",      // 速度と性能のバランス
                "claude-haiku-4-5-20251001" // 最高速・低コスト
        };
    }
}
