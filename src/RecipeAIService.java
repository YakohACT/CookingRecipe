import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 複数のAIプロバイダーを切り替えてレシピ提案を取得するサービスクラス
 */
public class RecipeAIService {

    /** AIプロバイダーの種類 */
    public enum Provider { OPENAI, GEMINI, CLAUDE }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";

    /**
     * プロバイダーとAPIキーの設定
     * @param provider 利用するAI
     * @param apiKey APIキー
     */
    public void setConfig(Provider provider, String apiKey) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
    }

    /**
     * AIによるレシピ提案の取得
     * @param allIngredients 候補食材リスト
     * @return 提案結果 [0]:タイトル, [1]:食材名（カンマ区切り）
     */
    public String[] suggestRecipe(ArrayList<Ingredient> allIngredients) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) throw new Exception("APIキーが設定されていません");

        String names = allIngredients.stream().map(Ingredient::getName).collect(Collectors.joining(","));
        String prompt = "優秀なシェフとして、以下の食材から3つ選びレシピを提案してください。" +
                "形式厳守。タイトル:[名前]\\n食材:[A],[B]\\nこれ以外の回答は不要。\\n候補:" + names;

        URL url;
        String json;

        // プロバイダーごとのリクエスト構築
        switch (selectedProvider) {
            case GEMINI:
                url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey);
                json = "{\"contents\": [{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";
                break;
            case CLAUDE:
                url = new URL("https://api.anthropic.com/v1/messages");
                json = "{\"model\":\"claude-3-haiku-20240307\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";
                break;
            case OPENAI:
            default:
                url = new URL("https://api.openai.com/v1/chat/completions");
                json = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";
                break;
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);

        // プロバイダー固有のヘッダー設定
        if (selectedProvider == Provider.OPENAI) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        } else if (selectedProvider == Provider.CLAUDE) {
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        StringBuilder res = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) res.append(line);
        }

        return parseResponse(res.toString());
    }

    /**
     * AIのJSONレスポンスからタイトルと食材の抽出
     * 各プロバイダーのJSON構造に合わせて解析
     */
    private String[] parseResponse(String json) {
        try {
            String text = "";
            if (selectedProvider == Provider.GEMINI) {
                int start = json.indexOf("\"text\": \"") + 9;
                int end = json.indexOf("\"", start);
                text = json.substring(start, end);
            } else if (selectedProvider == Provider.CLAUDE) {
                int start = json.indexOf("\"text\": \"") + 9;
                int end = json.indexOf("\"", start);
                text = json.substring(start, end);
            } else { // OpenAI
                int start = json.indexOf("\"content\": \"") + 12;
                int end = json.indexOf("\"", start);
                text = json.substring(start, end);
            }

            text = text.replace("\\n", "\n").replace("\\\"", "\"");
            String title = "AI提案レシピ", ings = "";
            for (String l : text.split("\n")) {
                if (l.contains("タイトル:")) title = l.split(":")[1].trim();
                if (l.contains("食材:")) ings = l.split(":")[1].trim();
            }
            return new String[]{title, ings};
        } catch (Exception e) {
            return new String[]{"AI提案レシピ", ""};
        }
    }
}