import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class OpenAIProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        // ダブルクォートのみエスケープ（\n はすでにJSON用エスケープ済みのため二重エスケープしない）
        String safePrompt = prompt.replace("\"", "\\\"");
        String json = "{\"model\":\"" + modelName + "\",\"messages\":"
                + "[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        // HTTPエラー時はエラーストリームから詳細メッセージを取得
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("OpenAI API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        String jsonRes = readStream(conn.getInputStream());

        // "content": "..." の開始位置を特定
        int start = jsonRes.indexOf("\"content\": \"");
        if (start == -1) {
            throw new Exception("レスポンス解析失敗: contentフィールドが見つかりません。レスポンス: " + jsonRes);
        }
        start += 12; // "content": " の文字数分進める

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
        throw new Exception("レスポンス解析失敗: contentフィールドの終端が見つかりません");
    }

    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "gpt-4o-mini",
                "gpt-4o",
                "gpt-3.5-turbo"
        };
    }
}
