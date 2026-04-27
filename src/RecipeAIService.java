import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Ollamaを利用してAIによるレシピ提案を行うサービスクラス
 */
public class RecipeAIService {
    // OllamaのローカルAPIエンドポイント
    private static final String API_URL = "http://localhost:11434/api/generate";
    // ユーザー指定のモデル名
    private static final String MODEL_NAME = "gemma4:e4b"; 

    /**
     * 利用可能な食材リストからAIにレシピを提案させる
     * @param allIngredients 利用可能なすべての食材リスト
     * @return AIが提案したレシピ情報（[0]にタイトル、[1]に食材名のカンマ区切り）
     */
    public String[] suggestRecipe(ArrayList<Ingredient> allIngredients) throws Exception {
        // 利用可能な食材のカンマ区切り文字列を作成
        String availableNames = allIngredients.stream()
                .map(Ingredient::getName)
                .collect(Collectors.joining(","));

        // AIへの指示（プロンプト）
        String prompt = "あなたは優秀なシェフだ。以下の「利用可能な食材」から3つ以上選び、レシピのタイトルと使用する食材を提案してほしい。" +
                        "出力形式は必ず以下のようにすること。これ以外の文章は出力しないこと。\\n" +
                        "タイトル:[レシピのタイトル]\\n" +
                        "食材:[食材名1],[食材名2]";

        // 送信するJSONデータの作成
        String jsonRequest = String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\\n\\n利用可能な食材:%s\",\"stream\":false}", 
            MODEL_NAME, prompt, availableNames
        );

        // HTTP通信の準備と送信
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonRequest.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // レスポンスの受け取り
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        return parseAiResponse(response.toString());
    }

    /**
     * AIのJSONレスポンスからタイトルと食材を抽出する
     * @param json OllamaからのJSON文字列
     * @return 抽出した文字列配列
     */
    private String[] parseAiResponse(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return new String[]{"AI提案レシピ", ""};
        start += key.length();
        
        int end = json.indexOf("\",\"done\"");
        if (end == -1) end = json.length() - 1;

        // エスケープされた改行文字などを元に戻す
        String text = json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");

        String title = "AI提案レシピ";
        String ingredients = "";

        // テキストを解析してタイトルと食材を取得
        for (String line : text.split("\n")) {
            if (line.startsWith("タイトル:")) {
                title = line.replace("タイトル:", "").trim();
            } else if (line.startsWith("食材:")) {
                ingredients = line.replace("食材:", "").trim();
            }
        }
        return new String[]{title, ingredients};
    }
}