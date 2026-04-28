import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Anthropic Claude との通信を担当するクラス
 */
public class ClaudeProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(allIngredients);
        String json = "{\"model\":\"claude-3-haiku-20240307\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
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