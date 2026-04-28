import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * OpenAI (GPT) との通信を担当するクラス
 */
public class OpenAIProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(allIngredients);
        String json = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes("utf-8")); }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) res.append(line);
        }

        String jsonRes = res.toString();
        int start = jsonRes.indexOf("\"content\": \"") + 12;
        int end = jsonRes.indexOf("\"", start);
        String text = jsonRes.substring(start, end);

        return parseStandardResponse(text);
    }
}