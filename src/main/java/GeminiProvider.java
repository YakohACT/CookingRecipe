import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Google Gemini との通信を担当するクラス
 */
public class GeminiProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(allIngredients);
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
        String json = "{\"contents\": [{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes("utf-8")); }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) res.append(line);
        }

        String jsonRes = res.toString();
        int start = jsonRes.indexOf("\"text\": \"") + 9;
        int end = jsonRes.indexOf("\"", start);
        String text = jsonRes.substring(start, end);

        return parseStandardResponse(text);
    }
}