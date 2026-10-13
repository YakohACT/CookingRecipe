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
        OPENAI, GEMINI, CLAUDE, OLLAMA, LLAMA_CPP;

        /**
         * このプロバイダーの利用にAPIキーが必要かを返す。
         * @return APIキーが必要なら true(ローカルで動く Ollama / llama.cpp は不要)
         */
        public boolean requiresApiKey() {
            return this != OLLAMA && this != LLAMA_CPP;
        }
    }

    private Provider selectedProvider = Provider.OPENAI;
    private String apiKey = "";
    private String modelName = "";
    private String llamaCppUrl = "";

    /**
     * 利用するプロバイダー・APIキー・モデル名を一括設定する。
     * @param provider  AIプロバイダー
     * @param apiKey    APIキー(ローカルLLMは空文字でよい)
     * @param modelName 利用モデル名
     */
    public void setConfig(Provider provider, String apiKey, String modelName) {
        this.selectedProvider = provider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * llama.cpp(llama-server)の接続先URLを設定する。
     * @param url llama-server のURL(例: http://localhost:9931)。未設定なら空文字
     */
    public void setLlamaCppUrl(String url) {
        this.llamaCppUrl = url;
    }

    /**
     * 設定済みプロバイダーへレシピ提案を依頼する。
     * APIキー(llama.cpp はサーバーURL)が未設定の場合は例外を投げる。
     * @param url            参照URL
     * @param allIngredients 利用可能な食材リスト
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     * @throws Exception API呼び出し失敗時 / 未設定時
     */
    public String[] suggestRecipe(String url, ArrayList<Ingredient> allIngredients) throws Exception {
        ensureConfigured();
        return createProvider(selectedProvider).generateRecipe(apiKey, modelName, url, allIngredients);
    }

    /**
     * 設定済みプロバイダーに、食材名の表記ゆれグループを検出させる。
     * APIキー(llama.cpp はサーバーURL)が未設定の場合は例外を投げる。
     * @param ingredientNames 現在登録されている全食材名
     * @return 表記ゆれグループのリスト(表記ゆれが無ければ空)
     * @throws Exception API呼び出し失敗時 / 未設定時
     */
    public List<IngredientNormalizer.VariantGroup> detectIngredientVariants(List<String> ingredientNames) throws Exception {
        ensureConfigured();
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
     * Ollama は localhost:11434/api/tags、llama.cpp は設定したURLの /v1/models、それ以外はプロバイダの models エンドポイントを叩く。
     * 失敗時は各プロバイダの静的フォールバックが返る(例外は呼び出し側へ漏れない)。
     * @param provider 対象プロバイダー
     * @param apiKey   APIキー(ローカルLLMは未使用、null/空文字許容)
     * @return モデル名配列
     */
    public String[] fetchModelsForProvider(Provider provider, String apiKey) {
        return createProvider(provider).fetchAvailableModels(apiKey);
    }

    /**
     * 呼び出しに必要な設定が揃っていなければ例外を投げる。
     * APIキーが必要なプロバイダーはキー、llama.cpp はサーバーURLが必要。
     * @throws Exception APIキー / サーバーURL 未設定時
     */
    private void ensureConfigured() throws Exception {
        if (selectedProvider.requiresApiKey() && (apiKey == null || apiKey.isEmpty())) {
            throw new Exception("APIキーが設定されていない");
        }
        if (selectedProvider == Provider.LLAMA_CPP && (llamaCppUrl == null || llamaCppUrl.trim().isEmpty())) {
            throw new Exception("llama.cpp のサーバーURLが設定されていない");
        }
    }

    /**
     * プロバイダーに対応する具象クラスを生成する。
     * @param provider AIプロバイダー
     * @return 対応する具象 AbstractRecipeAIProvider
     */
    private AbstractRecipeAIProvider createProvider(Provider provider) {
        switch (provider) {
            case GEMINI: return new GeminiProvider();
            case CLAUDE: return new ClaudeProvider();
            case OLLAMA: return new OllamaProvider();
            case LLAMA_CPP: return new LlamaCppProvider(llamaCppUrl);
            case OPENAI: default: return new OpenAIProvider();
        }
    }
}
