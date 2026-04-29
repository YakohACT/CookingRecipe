import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class OpenAIProvider extends AbstractRecipeAIProvider {
    @Override
    public String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(url, allIngredients);
        String json = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt.replace("\"", "\\\"") + "\"}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.openai.com/v1/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes("utf-8")); }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line; while ((line = br.readLine()) != null) res.append(line);
        }
        String jsonRes = res.toString();
        int start = jsonRes.indexOf("\"content\": \"") + 12;
        int end = jsonRes.indexOf("\"", start);
        return parseStandardResponse(jsonRes.substring(start, end));
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