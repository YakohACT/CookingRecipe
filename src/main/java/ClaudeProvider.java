import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClaudeProvider extends AbstractRecipeAIProvider {
    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        String json = "{\"model\":\"" + modelName + "\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt.replace("\"", "\\\"") + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes("utf-8")); }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line; while ((line = br.readLine()) != null) res.append(line);
        }
        String jsonRes = res.toString();
        int start = jsonRes.indexOf("\"text\": \"") + 9;
        int end = jsonRes.indexOf("\"", start);
        return parseStandardResponse(jsonRes.substring(start, end));
    }

    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "claude-3-haiku-20240307",
                "claude-3-sonnet-20240229",
                "claude-3-opus-20240229"
        };
    }
}