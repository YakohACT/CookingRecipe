package main.java.AI;

import java.util.prefs.Preferences;

/**
 * 各AIプロバイダー (OpenAI / Gemini / Claude) のAPIキーを
 * main.java.AI.SecureKeyStore で暗号化した上で Java Preferences に永続化する。
 *
 * Preferences のキー形式: "apiKey.<PROVIDER>" (例: apiKey.OPENAI)
 */
public class ApiKeyStore {

    private static final String PREF_PREFIX = "apiKey.";

    private final SecureKeyStore secure;
    private final Preferences prefs;

    /**
     * 暗号化エンジンと永続化先 Preferences をDIする。
     * @param secure 暗号化/復号を担う SecureKeyStore
     * @param prefs  保存先 Preferences ノード
     */
    public ApiKeyStore(SecureKeyStore secure, Preferences prefs) {
        this.secure = secure;
        this.prefs = prefs;
    }

    /**
     * 指定プロバイダーのAPIキーを取得する。未設定や復号失敗時は空文字を返す。
     * @param provider 取得したいプロバイダー
     * @return 復号済みのAPIキー(未設定/失敗時は空文字)
     */
    public String getKey(RecipeAIService.Provider provider) {
        if (provider == null) return "";
        String stored = prefs.get(PREF_PREFIX + provider.name(), "");
        if (stored.isEmpty()) return "";
        return secure.decrypt(stored);
    }

    /**
     * APIキーを暗号化して保存する。空文字を渡すとエントリを削除する。
     * @param provider 保存対象プロバイダー
     * @param plainKey 平文のAPIキー(空文字なら削除動作)
     */
    public void setKey(RecipeAIService.Provider provider, String plainKey) {
        if (provider == null) return;
        if (plainKey == null || plainKey.isEmpty()) {
            prefs.remove(PREF_PREFIX + provider.name());
            return;
        }
        String encrypted = secure.encrypt(plainKey);
        prefs.put(PREF_PREFIX + provider.name(), encrypted);
    }

    /** 全プロバイダーのキーを削除する(テストや初期化用) */
    public void clearAll() {
        for (RecipeAIService.Provider p : RecipeAIService.Provider.values()) {
            prefs.remove(PREF_PREFIX + p.name());
        }
    }
}
