package main.java.AI;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube の動画メタデータ(タイトル + 字幕)をテキスト化するヘルパー。
 * Ollama や OpenAI など動画自体を見られないモデル向けに、
 * AIプロンプトへ「テキスト化された動画コンテンツ」を注入するために使う。
 *
 * 取得方法:
 *  - タイトル: oEmbed API (https://www.youtube.com/oembed) — APIキー不要
 *  - 字幕   : watch ページに埋め込まれた ytInitialPlayerResponse から
 *             captionTracks[].baseUrl を抽出 → そのURLを叩いて timedtext XML を取得
 *
 * 言語優先度: ja > ja-JP > en > その他先頭。何も取れなければ空文字を返す
 */
public final class YoutubeMetadataFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_TRANSCRIPT_LENGTH = 4000;

    private YoutubeMetadataFetcher() {}

    /**
     * 動画タイトルと字幕をひとつのテキストブロックにまとめて返す。
     * 失敗時/取得不能時は空文字
     */
    public static String fetchSummary(String url) {
        String videoId = extractVideoId(url);
        if (videoId == null) return "";

        StringBuilder out = new StringBuilder();
        String title = fetchTitle(url);
        if (!title.isEmpty()) {
            out.append("【動画タイトル】\n").append(title).append("\n\n");
        }
        String transcript = fetchTranscript(videoId);
        if (!transcript.isEmpty()) {
            if (transcript.length() > MAX_TRANSCRIPT_LENGTH) {
                transcript = transcript.substring(0, MAX_TRANSCRIPT_LENGTH) + "…";
            }
            out.append("【動画字幕(抜粋)】\n").append(transcript);
        }
        return out.toString().trim();
    }

    private static String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m;
        m = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        m = Pattern.compile("youtube\\.com/shorts/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        m = Pattern.compile("youtube\\.com/embed/([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        m = Pattern.compile("[?&]v=([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    /** oEmbed で動画タイトルを取得 */
    private static String fetchTitle(String videoUrl) {
        try {
            String oembedUrl = "https://www.youtube.com/oembed?format=json&url="
                    + URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
            String json = httpGet(oembedUrl, "application/json");
            if (json.isEmpty()) return "";
            int keyIdx = json.indexOf("\"title\"");
            if (keyIdx == -1) return "";
            int colonIdx = json.indexOf(":", keyIdx);
            int valStart = json.indexOf("\"", colonIdx) + 1;
            int valEnd = findUnescapedQuote(json, valStart);
            if (valEnd <= valStart) return "";
            return jsonUnescape(json.substring(valStart, valEnd));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * watch ページに埋まっている ytInitialPlayerResponse から captionTracks を取り出し、
     * 適切な言語の baseUrl を叩いて timedtext XML を取得し、テキスト化する
     */
    private static String fetchTranscript(String videoId) {
        try {
            String html = httpGet("https://www.youtube.com/watch?v=" + videoId, "text/html");
            if (html.isEmpty()) return "";

            int idx = html.indexOf("\"captionTracks\":");
            if (idx == -1) return "";

            // 同一トラックエントリ内の baseUrl と languageCode をペアで抽出する
            String tail = html.substring(idx);
            Matcher m = Pattern.compile(
                    "\"baseUrl\"\\s*:\\s*\"([^\"]+)\"[^}]*\"languageCode\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(tail);

            Map<String, String> langToUrl = new LinkedHashMap<>();
            while (m.find()) {
                String baseUrl = m.group(1).replace("\\u0026", "&").replace("\\/", "/");
                String lang = m.group(2);
                langToUrl.putIfAbsent(lang, baseUrl);
            }
            if (langToUrl.isEmpty()) return "";

            // 言語優先度
            String chosen = null;
            for (String lang : new String[]{"ja", "ja-JP", "en", "en-US", "en-GB"}) {
                if (langToUrl.containsKey(lang)) {
                    chosen = langToUrl.get(lang);
                    break;
                }
            }
            if (chosen == null) chosen = langToUrl.values().iterator().next();

            String xml = httpGet(chosen, "text/xml");
            return parseTranscriptXml(xml);
        } catch (Exception e) {
            return "";
        }
    }

    /** timedtext の <text>...</text> を改行区切りで連結 */
    private static String parseTranscriptXml(String xml) {
        if (xml == null || xml.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("<text[^>]*>([\\s\\S]*?)</text>").matcher(xml);
        while (m.find()) {
            String chunk = m.group(1)
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&nbsp;", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!chunk.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(chunk);
            }
        }
        return sb.toString();
    }

    private static String httpGet(String url, String accept) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; RecipeManager/1.0)");
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("Accept-Language", "ja,en;q=0.9");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) return "";
        try (InputStream is = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }

    private static int findUnescapedQuote(String s, int pos) {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '\\') { pos += 2; continue; }
            if (c == '"') return pos;
            pos++;
        }
        return -1;
    }

    /** バックスラッシュ系のJSONエスケープ(quote, slash, n, t, ユニコード4桁等)を実体に戻す */
    private static String jsonUnescape(String s) {
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
                    case 'u':
                        if (i + 5 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                                sb.append((char) code);
                                i += 5;
                            } catch (NumberFormatException e) { sb.append(c); }
                        } else { sb.append(c); }
                        break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
