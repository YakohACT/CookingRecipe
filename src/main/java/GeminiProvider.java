import java.io.BufferedReader;
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
        String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

        String partsJson = "{\"text\":\"" + safePrompt + "\"}";

        if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
            String safeUrl = url.trim().replace("\"", "\\\"");
            partsJson += ", {\"file_data\": {\"file_uri\": \"" + safeUrl + "\"}}";
        }

        // 💡 引数で受け取った modelName を使用する
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + cleanKey;
        String json = "{\"contents\": [{\"parts\":[" + partsJson + "]}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("モデル通信エラー (HTTP " + responseCode + ")");
        }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) res.append(line);
        }

        String jsonRes = res.toString();
        int start = jsonRes.indexOf("\"text\":");
        if (start == -1) throw new Exception("レスポンス解析失敗");
        start = jsonRes.indexOf("\"", start + 7) + 1;
        int end = jsonRes.indexOf("\"", start);
        while (jsonRes.charAt(end - 1) == '\\') end = jsonRes.indexOf("\"", end + 1);

        return parseStandardResponse(jsonRes.substring(start, end));
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