import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Google Gemini との通信を担当するクラス
 */
public class GeminiProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String cleanKey = apiKey.trim();
        String prompt = buildPrompt(url, allIngredients);

        // buildPrompt の \n はすでにJSONエスケープ済みのため、バックスラッシュを二重エスケープしない。
        // ダブルクォートと制御文字のみエスケープする。
        String safePrompt = prompt.replace("\"", "\\\"").replace("\r", "");

        String partsJson = "{\"text\":\"" + safePrompt + "\"}";

        if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
            String safeUrl = url.trim().replace("\"", "\\\"");
            partsJson += ", {\"file_data\": {\"file_uri\": \"" + safeUrl + "\"}}";
        }

        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + cleanKey;
        String json = "{\"contents\": [{\"parts\":[" + partsJson + "]}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new Exception("Gemini API エラー (HTTP " + responseCode + "): " + errorBody);
        }

        String jsonRes = readStream(conn.getInputStream());

        int start = jsonRes.indexOf("\"text\":");
        if (start == -1) throw new Exception("レスポンス解析失敗: textフィールドが見つかりません");
        start = jsonRes.indexOf("\"", start + 7) + 1;

        // バックスラッシュエスケープを考慮して終端クォートを探す
        int end = findUnescapedQuote(jsonRes, start);

        return parseStandardResponse(jsonRes.substring(start, end));
    }

    /** ストリームを読み込んで文字列として返す。nullの場合は空文字を返す */
    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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
        return new String[]{
                "gemini-1.5-flash",
                "gemini-2.0-flash",
                "gemini-1.5-pro",
                "gemini-pro"
        };
    }
}
