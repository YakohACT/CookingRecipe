import java.util.ArrayList;

/**
 * ユーザー設定を保持し、各AIプロバイダーへ処理を委譲する橋渡しクラス
 */
public class RecipeAIService {

    public enum Provider { OPENAI, GEMINI, CLAUDE }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";
    private String modelName = "";

    public void setConfig(Provider provider, String apiKey, String modelName) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public String[] suggestRecipe(String url, ArrayList<Ingredient> allIngredients) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) throw new Exception("APIキーが設定されていない");

        AbstractRecipeAIProvider aiProvider;
        switch (selectedProvider) {
            case GEMINI: aiProvider = new GeminiProvider(); break;
            case CLAUDE: aiProvider = new ClaudeProvider(); break;
            case OPENAI: default: aiProvider = new OpenAIProvider(); break;
        }

        // modelName も一緒に渡す
        return aiProvider.generateRecipe(apiKey, modelName, url, allIngredients);
    }

    /**
     * 指定されたプロバイダーの利用可能なモデル一覧を取得する
     * @param provider 対象のAIプロバイダー
     * @return モデル名の配列
     */
    public String[] getModelsForProvider(Provider provider) {
        AbstractRecipeAIProvider aiProvider;
        switch (provider) {
            case GEMINI: aiProvider = new GeminiProvider(); break;
            case CLAUDE: aiProvider = new ClaudeProvider(); break;
            case OPENAI: default: aiProvider = new OpenAIProvider(); break;
        }
        return aiProvider.getAvailableModels();
    }
}