import java.util.ArrayList;

/**
 * ユーザー設定を保持し、各AIプロバイダーへ処理を委譲する橋渡しクラス
 */
public class RecipeAIService {

    public enum Provider { OPENAI, GEMINI, CLAUDE, OLLAMA }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";
    private String modelName = "";

    public void setConfig(Provider provider, String apiKey, String modelName) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public String[] suggestRecipe(String url, ArrayList<Ingredient> allIngredients) throws Exception {
        // OllamaはローカルLLMなのでAPIキー不要
        if (selectedProvider != Provider.OLLAMA && (apiKey == null || apiKey.isEmpty())) {
            throw new Exception("APIキーが設定されていない");
        }

        AbstractRecipeAIProvider aiProvider = createProvider(selectedProvider, modelName);

        // modelName も一緒に渡す
        return aiProvider.generateRecipe(apiKey, modelName, url, allIngredients);
    }

    /**
     * 指定されたプロバイダーの利用可能なモデル一覧を取得する。
     * Ollama の場合は全サブクラスのモデルを集約して返す
     * @param provider 対象のAIプロバイダー
     * @return モデル名の配列
     */
    public String[] getModelsForProvider(Provider provider) {
        if (provider == Provider.OLLAMA) {
            ArrayList<String> all = new ArrayList<>();
            for (OllamaProvider p : ollamaSubclasses()) {
                for (String m : p.getAvailableModels()) all.add(m);
            }
            return all.toArray(new String[0]);
        }
        return createProvider(provider, "").getAvailableModels();
    }

    /**
     * プロバイダー(と Ollama の場合は modelName)から具象クラスを生成する
     */
    private static AbstractRecipeAIProvider createProvider(Provider provider, String modelName) {
        switch (provider) {
            case GEMINI: return new GeminiProvider();
            case CLAUDE: return new ClaudeProvider();
            case OLLAMA: return ollamaProviderForModel(modelName);
            case OPENAI: default: return new OpenAIProvider();
        }
    }

    /**
     * モデル名のプレフィックスで Ollama サブクラスを振り分ける。
     * 不明なモデル名は LlamaProvider をフォールバックとして利用(API挙動は同じ)
     */
    private static OllamaProvider ollamaProviderForModel(String modelName) {
        String lower = (modelName == null) ? "" : modelName.toLowerCase();
        if (lower.startsWith("gemma"))                                  return new GemmaProvider();
        if (lower.startsWith("mistral") || lower.startsWith("mixtral")) return new MistralProvider();
        if (lower.startsWith("phi"))                                    return new PhiProvider();
        if (lower.startsWith("qwen") || lower.startsWith("qwq")) return new QwenProvider();
        return new LlamaProvider();
    }

    /** Ollama 系サブクラスの一覧(モデル列挙用) */
    private static OllamaProvider[] ollamaSubclasses() {
        return new OllamaProvider[]{
                new LlamaProvider(),
                new GemmaProvider(),
                new MistralProvider(),
                new PhiProvider(),
                new QwenProvider()
        };
    }
}
