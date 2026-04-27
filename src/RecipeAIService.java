import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Ollama APIと通信しレシピ提案を取得するサービスクラス
 */
public class RecipeAIService {
    private static final String API_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "gemma4:e4b";

    /**
     * AIによるレシピ提案の取得
     * @param allIngredients 候補となる食材リスト
     * @return 提案結果 [0]:タイトル, [1]:食材名（カンマ区切り）
     */
    public String[] suggestRecipe(ArrayList<Ingredient> allIngredients) throws Exception {
        String names = allIngredients.stream().map(Ingredient::getName).collect(Collectors.joining(","));
        String prompt = "優秀なシェフとして、以下の食材から3つ選びレシピを提案してください。" +
                "形式厳守。タイトル:[名前]\\n食材:[A],[B]\\nこれ以外の回答は不要。\\n候補:" + names;

        String json = String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}", MODEL_NAME, prompt);

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes("utf-8")); }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) res.append(line);
        }
        return parseResponse(res.toString());
    }

    private String[] parseResponse(String json) {
        int start = json.indexOf("\"response\":\"") + 12;
        int end = json.indexOf("\",\"done\"");
        String text = json.substring(start, end).replace("\\n", "\n");
        String title = "AI提案レシピ", ings = "";
        for (String l : text.split("\n")) {
            if (l.contains("タイトル:")) title = l.split(":")[1].trim();
            if (l.contains("食材:")) ings = l.split(":")[1].trim();
        }
        return new String[]{title, ings};
    }
}