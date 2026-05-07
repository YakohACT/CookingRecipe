package main.java.AI;

import main.java.Recipe.Ingredient;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 各AIプロバイダーの共通処理をまとめた抽象クラス。
 * プロンプト構築 / JSONエスケープ / レスポンスJSONのパースを提供する
 */
public abstract class AbstractRecipeAIProvider {

    /**
     * 各プロバイダー固有のAPIを呼び出してレシピ案を取得する。
     * @param apiKey         APIキー(Ollamaは未使用)
     * @param modelName      モデル名(例: gpt-4.1, gemini-2.5-flash, llama3:8b 等)
     * @param url            レシピ参照URL(YouTube/Webページ)
     * @param allIngredients 利用可能な食材リスト
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     * @throws Exception API呼び出し/パース失敗時
     */
    public abstract String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception;

    /**
     * このプロバイダーで選択可能なモデル名の配列を返す(静的フォールバック)。
     * @return モデル名配列
     */
    public abstract String[] getAvailableModels();

    /**
     * APIキーを使って動的にモデル一覧を取得する。
     * デフォルト実装は {@link #getAvailableModels()} の静的フォールバックを返す。
     * 各プロバイダーは必要に応じてオーバーライドしてHTTPリストAPIを叩く。
     * 失敗時は静的フォールバックを返すこと(例外を呼び出し側へ漏らさない)。
     * @param apiKey APIキー(Ollamaは未使用、null/空文字許容)
     * @return 動的取得できたモデル名配列。失敗時はフォールバック
     */
    public String[] fetchAvailableModels(String apiKey) {
        return getAvailableModels();
    }

    /**
     * 食材候補からレシピを提案させるプロンプトを構築する。
     * 候補リストを先頭に置き、JSON形式での回答を強制する。
     * @param url            参照URL(空文字許容)
     * @param allIngredients 利用可能な食材リスト
     * @return AIへ送信するプロンプト文字列
     */
    protected String buildPrompt(String url, ArrayList<Ingredient> allIngredients) {
        String names = allIngredients.stream()
                .map(Ingredient::getName)
                .collect(Collectors.joining(", "));

        boolean isYoutube = url.contains("youtube.com") || url.contains("youtu.be");
        // 参考情報の取得:
        //   - YouTube URL → oEmbedで動画タイトル + watchページから字幕を取得
        //     (Geminiは別途file_dataで動画自体も渡すが、Ollama等の動画非対応モデルへの保険)
        //   - YouTube以外 → HTMLを取得してテキスト化(main.java.AI.WebPageFetcher)
        String pageText = "";
        if (!url.trim().isEmpty()) {
            pageText = isYoutube
                    ? YoutubeMetadataFetcher.fetchSummary(url)
                    : WebPageFetcher.fetchSummary(url);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("あなたは日本の家庭料理のシェフです。\n\n");
        sb.append("【利用可能な食材】\n");
        sb.append(names).append("\n\n");
        if (!pageText.isEmpty()) {
            sb.append(isYoutube ? "【参考動画(指定URLから取得)】\n" : "【参考レシピページ(指定URLから取得)】\n");
            sb.append(pageText).append("\n\n");
        }
        sb.append("【ルール】\n");
        sb.append("・上記リストに含まれる食材だけを使うこと\n");
        sb.append("・前置き・補足・Markdown装飾・コードフェンスは一切禁止\n");
        if (isYoutube) {
            if (!pageText.isEmpty()) {
                sb.append("・上記の動画タイトル/字幕の料理を再現するように料理名と食材を選ぶこと\n");
            } else {
                // タイトル/字幕が取れなかったケース: Gemini系の動画ネイティブ理解にフォールバック
                sb.append("・動画の内容を参考にすること\n");
            }
        } else if (!pageText.isEmpty()) {
            sb.append("・参考レシピページの料理を再現するように料理名と食材を選ぶこと\n");
        }
        sb.append("\n【出力形式】\n");
        sb.append("以下のキーを持つJSONオブジェクトのみを返してください。\n");
        sb.append("- title: 料理名 (文字列)\n");
        sb.append("- ingredients: 上記リストから選んだ食材名 (文字列の配列)\n");
        return sb.toString();
    }

    /**
     * APIレスポンスから抽出した text/content フィールドの中身(まだJSONエスケープ済み)を
     * 解読し、{title, ingredients(カンマ連結)} の形に変換する。
     * @param escapedJsonText API応答内のJSON文字列(エスケープ済み)
     * @return [タイトル, "食材1,食材2,…"] の2要素配列
     */
    protected String[] parseJsonResponse(String escapedJsonText) {
        String text = jsonUnescape(escapedJsonText);
        // フォールバックを空文字にすることで、UI側で「AIがレシピを検出できなかった」状態を判定できるようにする
        String title = extractJsonString(text, "title", "");
        String[] ings = extractJsonStringArray(text, "ingredients");

        StringBuilder joined = new StringBuilder();
        for (String ing : ings) {
            String cleaned = stripBrackets(ing);
            if (cleaned.isEmpty()) continue;
            if (joined.length() > 0) joined.append(",");
            joined.append(cleaned);
        }
        return new String[]{stripBrackets(title), joined.toString()};
    }

    /**
     * "key":"value" 形式の文字列値を抽出する。
     * @param text     検索対象のJSON文字列(unescape済み)
     * @param key      探したいキー名
     * @param fallback 見つからない場合のデフォルト値
     * @return 抽出した文字列値、または fallback
     */
    private String extractJsonString(String text, String key, String fallback) {
        int keyIdx = text.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return fallback;
        int colonIdx = text.indexOf(":", keyIdx);
        if (colonIdx == -1) return fallback;
        int valStart = text.indexOf("\"", colonIdx);
        if (valStart == -1) return fallback;
        valStart++;
        int valEnd = findUnescapedQuote(text, valStart);
        if (valEnd == -1) return fallback;
        return text.substring(valStart, valEnd);
    }

    /**
     * "key":["v1","v2",...] 形式の文字列配列を抽出する。
     * @param text 検索対象のJSON文字列(unescape済み)
     * @param key  探したいキー名
     * @return 抽出した文字列配列(見つからなければ空配列)
     */
    private String[] extractJsonStringArray(String text, String key) {
        int keyIdx = text.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return new String[0];
        int arrStart = text.indexOf("[", keyIdx);
        if (arrStart == -1) return new String[0];
        int arrEnd = text.indexOf("]", arrStart);
        if (arrEnd == -1) return new String[0];

        ArrayList<String> items = new ArrayList<>();
        int pos = arrStart + 1;
        while (pos < arrEnd) {
            int strStart = text.indexOf("\"", pos);
            if (strStart == -1 || strStart >= arrEnd) break;
            int strEnd = findUnescapedQuote(text, strStart + 1);
            if (strEnd == -1 || strEnd > arrEnd) break;
            items.add(text.substring(strStart + 1, strEnd));
            pos = strEnd + 1;
        }
        return items.toArray(new String[0]);
    }

    /**
     * 文字列 s の指定位置以降でエスケープされていない最初の '"' を探す。
     * @param s   走査対象の文字列
     * @param pos 走査開始位置(この位置から探す)
     * @return 見つかった '"' のインデックス。見つからなければ -1
     */
    protected static int findUnescapedQuote(String s, int pos) {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '\\') {
                pos += 2;
                continue;
            }
            if (c == '"') return pos;
            pos++;
        }
        return -1;
    }

    /**
     * Java文字列をJSON文字列値として埋め込めるようエスケープする。
     * @param s 元の文字列
     * @return JSON文字列値として安全な形にエスケープした文字列
     */
    protected static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * JSON文字列値内のエスケープを解除する({@code \"} → {@code "} など)。
     * @param s エスケープされたJSON文字列値
     * @return 実体を復元した文字列
     */
    protected static String jsonUnescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\\': sb.append('\\'); i++; break;
                    case '"':  sb.append('"');  i++; break;
                    case '/':  sb.append('/');  i++; break;
                    case 'n':  sb.append('\n'); i++; break;
                    case 'r':  sb.append('\r'); i++; break;
                    case 't':  sb.append('\t'); i++; break;
                    case 'b':  sb.append('\b'); i++; break;
                    case 'f':  sb.append('\f'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                                sb.append((char) code);
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * JSON 文字列内に出現する `"key":"value"` パターンの value をすべて収集する。
     * モデル一覧API のように `[{"id":"..."},{"id":"..."},...]` の "id" を全件取りたい時に使う。
     * @param json 検索対象 JSON 文字列
     * @param key  対象キー名
     * @return 抽出された値のリスト(順序保持、unescape 済み)
     */
    protected static java.util.List<String> extractAllJsonStrings(String json, String key) {
        java.util.List<String> results = new java.util.ArrayList<>();
        if (json == null || key == null) return results;
        String pattern = "\"" + key + "\"";
        int pos = 0;
        while (true) {
            int keyIdx = json.indexOf(pattern, pos);
            if (keyIdx < 0) break;
            int colonIdx = json.indexOf(":", keyIdx + pattern.length());
            if (colonIdx < 0) break;
            int valStart = json.indexOf("\"", colonIdx);
            if (valStart < 0) break;
            valStart++;
            int valEnd = findUnescapedQuote(json, valStart);
            if (valEnd < 0) break;
            results.add(jsonUnescape(json.substring(valStart, valEnd)));
            pos = valEnd + 1;
        }
        return results;
    }

    /**
     * AIがプレースホルダや装飾を含めた場合のために角括弧・カギカッコ・先頭の箇条書き記号などを除去する。
     * @param s 元の文字列
     * @return 装飾を除去した文字列
     */
    protected String stripBrackets(String s) {
        return s.replaceAll("[\\[\\]【】「」]", "")
                .replaceAll("^[\\s\\-*\\u30FB・]+", "")
                .trim();
    }
}
