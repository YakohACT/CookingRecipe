package main.java.AI;

import main.java.Recipe.Ingredient;
import main.java.Recipe.IngredientNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * ユーザー設定を保持し、各AIプロバイダーへ処理を委譲する橋渡しクラス
 */
public class RecipeAIService {

    public enum Provider {
        OPENAI, GEMINI, CLAUDE, OLLAMA;

        /**
         * このプロバイダーの利用にAPIキーが必要かを返す。
         * @return APIキーが必要なら true(ローカルで動く Ollama のみ不要)
         */
        public boolean requiresApiKey() {
            return this != OLLAMA;
        }
    }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";
    private String modelName = "";

    /**
     * 利用するプロバイダー・APIキー・モデル名を一括設定する。
     * @param provider  AIプロバイダー
     * @param apiKey    APIキー(Ollamaは空文字でよい)
     * @param modelName 利用モデル名
     */
    public void setConfig(Provider provider, String apiKey, String modelName) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * 設定済みプロバイダーへレシピ提案を依頼する。
     * Ollama 以外で APIキー未設定の場合は例外を投げる。
     * @param url            参照URL
     * @param allIngredients 利用可能な食材リスト
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     * @throws Exception API呼び出し失敗時 / APIキー未設定時
     */
    public String[] suggestRecipe(String url, ArrayList<Ingredient> allIngredients) throws Exception {
        ensureApiKey();
        return createProvider(selectedProvider).generateRecipe(apiKey, modelName, url, allIngredients);
    }

    /**
     * 設定済みプロバイダーに、食材名の表記ゆれグループを検出させる。
     * Ollama 以外で APIキー未設定の場合は例外を投げる。
     * @param ingredientNames 現在登録されている全食材名
     * @return 表記ゆれグループのリスト(表記ゆれが無ければ空)
     * @throws Exception API呼び出し失敗時 / APIキー未設定時
     */
    public List<IngredientNormalizer.VariantGroup> detectIngredientVariants(List<String> ingredientNames) throws Exception {
        ensureApiKey();
        String prompt = IngredientNormalizer.buildPrompt(ingredientNames);
        String json = createProvider(selectedProvider).chat(apiKey, modelName, prompt);
        return IngredientNormalizer.parseGroups(json);
    }

    /**
     * 指定されたプロバイダーの静的なモデル一覧(フォールバック用)を取得する。
     * @param provider 対象のAIプロバイダー
     * @return モデル名の配列
     */
    public String[] getModelsForProvider(Provider provider) {
        return createProvider(provider).getAvailableModels();
    }

    /**
     * 動的にプロバイダのAPIへ問い合わせて利用可能なモデル一覧を取得する。
     * Ollama は localhost:11434/api/tags、それ以外はプロバイダの models エンドポイントを叩く。
     * 失敗時は各プロバイダの静的フォールバックが返る(例外は呼び出し側へ漏れない)。
     * @param provider 対象プロバイダー
     * @param apiKey   APIキー(Ollamaは未使用、null/空文字許容)
     * @return モデル名配列
     */
    public String[] fetchModelsForProvider(Provider provider, String apiKey) {
        return createProvider(provider).fetchAvailableModels(apiKey);
    }

    /**
     * APIキーが必要なプロバイダーでキーが未設定なら例外を投げる。
     * @throws Exception APIキー未設定時
     */
    private void ensureApiKey() throws Exception {
        if (selectedProvider.requiresApiKey() && (apiKey == null || apiKey.isEmpty())) {
            throw new Exception("APIキーが設定されていない");
        }
    }

    /**
     * プロバイダーに対応する具象クラスを生成する。
     * @param provider AIプロバイダー
     * @return 対応する具象 AbstractRecipeAIProvider
     */
    private static AbstractRecipeAIProvider createProvider(Provider provider) {
        switch (provider) {
            case GEMINI: return new GeminiProvider();
            case CLAUDE: return new ClaudeProvider();
            case OLLAMA: return new OllamaProvider();
            case OPENAI: default: return new OpenAIProvider();
        }
    }
}
