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
    /** 選択可能なAI */
    public enum Provider { OPENAI, GEMINI, CLAUDE }
    /** 選択したAI */
    private Provider selectedProvider = Provider.OPENAI;
    /** APIキー */
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
        if (apiKey == null || apiKey.isEmpty()) throw new Exception("APIキーが設定されていない");

        String names = allIngredients.stream().map(Ingredient::getName).collect(Collectors.joining(","));
        String prompt = "優秀なシェフとして、以下の食材から3つ選びレシピを提案してください。" +
                "形式厳守。タイトル:[名前]\\n食材:[A],[B]\\nこれ以外の回答や記号（ダブルクォーテーション等）は一切不要。\\n候補:" + names;

        URL url;
        String json;

        // プロバイダーごとのリクエスト構築
        switch (selectedProvider) {
            case GEMINI:
                // 最新かつ高速な gemini-1.5-flash を利用
                url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
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

        // リクエストの送信
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }

        // レスポンスコードの確認（エラーの早期発見）
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // エラーの詳細を読み取る
            try (BufferedReader errorBr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                String errorMsg = errorBr.lines().collect(Collectors.joining());
                System.err.println("APIエラー詳細: " + errorMsg); // コンソールに詳細を出力
                throw new Exception("HTTPエラー " + responseCode + " (詳細はコンソールを確認)");
            }
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
     * 外部ライブラリなしでもエラーが起きにくい「キーワード直接検索型」の解析を実行
     */
    private String[] parseResponse(String json) {
        try {
            // エスケープされた改行などを実際の文字に変換
            String unescaped = json.replace("\\n", "\n").replace("\\\"", "\"");

            String title = "AI提案レシピ";
            String ings = "";

            // JSONの厳密な構造に依存せず、必要な行だけを直接抜き出す（堅牢な処理）
            for (String line : unescaped.split("\n")) {
                if (line.contains("タイトル:")) {
                    // 「タイトル:」以降の文字を取得し、邪魔なJSONの記号( " や , や } )を消去
                    title = line.substring(line.indexOf("タイトル:") + 5).replaceAll("[\",\\}\\]]", "").trim();
                } else if (line.contains("食材:")) {
                    ings = line.substring(line.indexOf("食材:") + 3).replaceAll("[\",\\}\\]]", "").trim();
                }
            }
            return new String[]{title, ings};
        } catch (Exception e) {
            return new String[]{"AI提案レシピ", ""};
        }
    }
}