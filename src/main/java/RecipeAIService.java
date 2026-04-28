import java.util.ArrayList;

/**
 * ユーザー設定を保持し、各AIプロバイダーへ処理を委譲する橋渡し（Context）クラス
 */
public class RecipeAIService {

    /** AIプロバイダーの種類 */
    public enum Provider { OPENAI, GEMINI, CLAUDE }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";

    /**
     * プロバイダーとAPIキーの設定
     */
    public void setConfig(Provider provider, String apiKey) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
    }

    /**
     * 設定されたAIを利用してレシピ提案の取得
     */
    public String[] suggestRecipe(ArrayList<Ingredient> allIngredients) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("APIキーが設定されていない");
        }

        AbstractRecipeAIProvider aiProvider;

        // 選択されたプロバイダーに応じてインスタンスを切り替え（Strategyパターン）
        switch (selectedProvider) {
            case GEMINI:
                aiProvider = new GeminiProvider();
                break;
            case CLAUDE:
                aiProvider = new ClaudeProvider();
                break;
            case OPENAI:
            default:
                aiProvider = new OpenAIProvider();
                break;
        }

        // 生成処理は各専用クラスに一任
        return aiProvider.generateRecipe(apiKey, allIngredients);
    }
}