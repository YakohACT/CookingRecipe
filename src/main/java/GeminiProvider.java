import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.util.ArrayList;

/**
 * Google Gemini との通信を担当するクラス
 * 公式SDK (google-genai) を使用した実装
 */
public class GeminiProvider extends AbstractRecipeAIProvider {

    @Override
    public String[] generateRecipe(String apiKey, ArrayList<Ingredient> allIngredients) throws Exception {
        String prompt = buildPrompt(allIngredients);

        // 1. クライアントの初期化（UIから受け取ったAPIキーを設定）
        System.setProperty("GEMINI_API_KEY", apiKey);
        Client client = new Client();

        // 2. generateContent メソッドによるリクエスト
        GenerateContentResponse response = client.models.generateContent(
                "gemini-1.5-flash",
                prompt,
                null
        );

        // 3. レスポンスからテキストを取得し、共通の解析メソッドへ渡す
        String text = response.text();
        return parseStandardResponse(text);
    }
}