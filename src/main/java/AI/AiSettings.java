package main.java.AI;

import java.util.prefs.Preferences;

/**
 * AI利用設定(プロバイダー・APIキー・モデル・llama.cpp のURL)を Java Preferences に保存/復元するクラス。
 * APIキーは {@link SecureKeyStore} で暗号化してから保存する。
 * 設定済みの内容で {@link RecipeAIService} を生成する役割も持つ。
 */
public class AiSettings {

    private static final String PREF_PROVIDER = "aiProvider";
    private static final String PREF_MODEL_PREFIX = "aiModel.";
    private static final String PREF_API_KEY_PREFIX = "apiKey.";
    private static final String PREF_LLAMA_CPP_URL = "llamaCppUrl";

    private final Preferences prefs;
    private final SecureKeyStore secure;

    private RecipeAIService.Provider provider = RecipeAIService.Provider.OPENAI;

    /**
     * 保存先 Preferences を受け取り、前回選択していたプロバイダーを復元する。
     * @param prefs 設定の保存先 Preferences ノード
     */
    public AiSettings(Preferences prefs) {
        this.prefs = prefs;
        this.secure = new SecureKeyStore();
        String savedProvider = prefs.get(PREF_PROVIDER, "");
        if (!savedProvider.isEmpty()) {
            try {
                provider = RecipeAIService.Provider.valueOf(savedProvider);
            } catch (IllegalArgumentException ignore) {}
        }
    }

    /**
     * @return 現在選択中のAIプロバイダー
     */
    public RecipeAIService.Provider getProvider() { return provider; }

    /**
     * 現在のAIプロバイダーを切替えて保存する。
     * @param p 切替先のプロバイダー
     */
    public void setProvider(RecipeAIService.Provider p) {
        this.provider = p;
        if (p != null) prefs.put(PREF_PROVIDER, p.name());
    }

    /**
     * 指定プロバイダーのAPIキーを復号して返す。
     * @param p 対象プロバイダー
     * @return 復号済みのAPIキー(未設定/復号失敗時は空文字)
     */
    public String getApiKey(RecipeAIService.Provider p) {
        if (p == null) return "";
        String stored = prefs.get(PREF_API_KEY_PREFIX + p.name(), "");
        if (stored.isEmpty()) return "";
        return secure.decrypt(stored);
    }

    /**
     * 指定プロバイダーのAPIキーを暗号化して保存する。空文字なら削除する。
     * @param p        対象プロバイダー
     * @param plainKey 平文のAPIキー(空文字で削除)
     */
    public void setApiKey(RecipeAIService.Provider p, String plainKey) {
        if (p == null) return;
        if (plainKey == null || plainKey.isEmpty()) {
            prefs.remove(PREF_API_KEY_PREFIX + p.name());
            return;
        }
        prefs.put(PREF_API_KEY_PREFIX + p.name(), secure.encrypt(plainKey));
    }

    /**
     * 指定プロバイダーで保存済みのモデル名を返す。
     * @param p 対象プロバイダー
     * @return 保存値があればそれ、無ければそのプロバイダーの先頭モデル
     */
    public String getModel(RecipeAIService.Provider p) {
        if (p == null) return "";
        String saved = prefs.get(PREF_MODEL_PREFIX + p.name(), "");
        if (!saved.isEmpty()) return saved;
        // 未設定のときはそのプロバイダーの最初の利用可能モデルにフォールバック
        String[] models = RecipeAIService.defaultModels(p);
        return models.length > 0 ? models[0] : "";
    }

    /**
     * 指定プロバイダーの利用モデル名を保存する。空文字なら削除する。
     * @param p     対象プロバイダー
     * @param model 保存するモデル名(空文字で削除)
     */
    public void setModel(RecipeAIService.Provider p, String model) {
        if (p == null) return;
        if (model == null || model.isEmpty()) {
            prefs.remove(PREF_MODEL_PREFIX + p.name());
        } else {
            prefs.put(PREF_MODEL_PREFIX + p.name(), model);
        }
    }

    /**
     * @return llama.cpp(llama-server)の接続先URL。未設定(初回起動時)なら空文字
     */
    public String getLlamaCppUrl() {
        return prefs.get(PREF_LLAMA_CPP_URL, "");
    }

    /**
     * llama.cpp(llama-server)の接続先URLを保存する。空文字なら削除して未設定に戻す。
     * @param url 保存するURL
     */
    public void setLlamaCppUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            prefs.remove(PREF_LLAMA_CPP_URL);
        } else {
            prefs.put(PREF_LLAMA_CPP_URL, url.trim());
        }
    }

    /**
     * 現在のプロバイダーでAIを呼び出せる状態かを返す。
     * @return 必要なAPIキー / サーバーURL が揃っていれば true
     */
    public boolean isReady() {
        return RecipeAIService.isConfigured(provider, getApiKey(provider), getLlamaCppUrl());
    }

    /**
     * 現在の設定(プロバイダー・APIキー・モデル・llama.cpp URL)で RecipeAIService を生成する。
     * @return 設定済みの RecipeAIService
     */
    public RecipeAIService createService() {
        return new RecipeAIService(provider, getApiKey(provider), getModel(provider), getLlamaCppUrl());
    }
}
