package main.java.AI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * YouTube に字幕が無い場合のフォールバック転写。
 * yt-dlp で音声を 16kHz/mono WAV に抽出し、whisper.cpp(whisper-cli)で文字起こしする。
 * 必要な実行ファイル一式は setup-whisper.bat で取得する想定。
 *
 * 配置:
 *   lib/tools/yt-dlp.exe
 *   lib/tools/ffmpeg.exe
 *   lib/tools/whisper-cli.exe   (または main.exe)
 *   lib/models/ggml-base.bin    (14M〜の各サイズに差替可)
 */
public final class WhisperTranscriber {

    /** 1動画あたりの最大処理時間。CPUでも完了するよう余裕を持たせる */
    private static final long TRANSCRIBE_TIMEOUT_SEC = 600; // 10分

    private WhisperTranscriber() {}

    /**
     * Whisper パイプラインの全構成要素が揃っているかを返す。
     * @return yt-dlp / ffmpeg / whisper-cli / モデルが全て見つかれば true
     */
    public static boolean isAvailable() {
        return locateTool("yt-dlp.exe") != null
                && locateTool("ffmpeg.exe") != null
                && locateWhisperCli() != null
                && locateModel() != null;
    }

    /**
     * YouTube URL から音声を抽出して文字起こしを行う。
     * 失敗時は空文字を返し、呼び出し側のフローを止めない。
     * @param youtubeUrl 対象動画のURL
     * @return 文字起こしテキスト(失敗時は空文字)
     */
    public static String transcribe(String youtubeUrl) {
        if (!isAvailable()) {
            System.err.println("[WhisperTranscriber] パイプライン未セットアップ: setup-whisper.bat を実行してください");
            return "";
        }
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) return "";

        Path ytDlp       = locateTool("yt-dlp.exe");
        Path ffmpegExe   = locateTool("ffmpeg.exe");
        Path whisperCli  = locateWhisperCli();
        Path modelFile   = locateModel();
        Path toolsDir    = ytDlp.getParent();

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("recipe-whisper-");
            Path audioStem = tempDir.resolve("audio");

            // === 1. yt-dlp で 16kHz mono WAV を取得 =======================
            // --postprocessor-args は ffmpeg に渡される
            int rc = runProcess(toolsDir, TRANSCRIBE_TIMEOUT_SEC,
                    ytDlp.toString(),
                    "-x",
                    "--audio-format", "wav",
                    "--ffmpeg-location", ffmpegExe.getParent().toString(),
                    "--postprocessor-args", "-ar 16000 -ac 1",
                    "--no-playlist",
                    "--quiet",
                    "-o", audioStem + ".%(ext)s",
                    youtubeUrl);
            if (rc != 0) {
                System.err.println("[WhisperTranscriber] yt-dlp 失敗 (rc=" + rc + ")");
                return "";
            }

            Path wav = tempDir.resolve("audio.wav");
            if (!Files.exists(wav)) {
                System.err.println("[WhisperTranscriber] 音声ファイルが生成されませんでした: " + wav);
                return "";
            }

            // === 2. whisper-cli で文字起こし =============================
            // -otxt で <wavPath>.txt が出力される
            rc = runProcess(toolsDir, TRANSCRIBE_TIMEOUT_SEC,
                    whisperCli.toString(),
                    "-m", modelFile.toString(),
                    "-f", wav.toString(),
                    "-l", "auto",        // 言語自動判定
                    "-otxt",
                    "-nt");              // タイムスタンプ非表示でテキストだけにする
            if (rc != 0) {
                System.err.println("[WhisperTranscriber] whisper-cli 失敗 (rc=" + rc + ")");
                return "";
            }

            Path outTxt = Paths.get(wav.toString() + ".txt");
            if (!Files.exists(outTxt)) {
                System.err.println("[WhisperTranscriber] 文字起こし結果ファイルが見つかりません: " + outTxt);
                return "";
            }

            String text = Files.readString(outTxt, StandardCharsets.UTF_8).trim();
            return text;
        } catch (Exception e) {
            System.err.println("[WhisperTranscriber] 例外: " + e.getMessage());
            return "";
        } finally {
            if (tempDir != null) deleteRecursively(tempDir);
        }
    }

    /**
     * 実行ファイルを引数列で起動して終了を待つ。
     * 標準出力は破棄、エラーストリームのみコンソールへ転送する。
     * @param workingDir   起動時の作業ディレクトリ(PATH 先頭にも追加される)
     * @param timeoutSec   タイムアウト秒数
     * @param command      実行コマンドと引数
     * @return 終了コード(タイムアウト時は -1)
     */
    private static int runProcess(Path workingDir, long timeoutSec, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        // PATH を先頭に追加して、yt-dlp が ffmpeg を発見できるようにする
        Map<String, String> env = pb.environment();
        String oldPath = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
        env.put("Path", workingDir.toString() + File.pathSeparator + oldPath);
        // 標準出力は破棄。標準エラーは inherit してデバッグ実行時に見えるように
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process p = pb.start();
        boolean done = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!done) {
            p.destroyForcibly();
            return -1;
        }
        return p.exitValue();
    }

    // ---- 配置検出 ----------------------------------------------------------

    /**
     * lib/tools/ の候補ディレクトリを探索して指定ファイル名の絶対パスを返す。
     * @param exeName 探したい実行ファイル名(例: "yt-dlp.exe")
     * @return 見つかった Path。見つからなければ null
     */
    private static Path locateTool(String exeName) {
        for (Path base : toolsDirCandidates()) {
            Path p = base.resolve(exeName);
            if (Files.exists(p)) return p.toAbsolutePath();
        }
        return null;
    }

    /**
     * whisper-cli.exe または(古いビルドの)main.exe を探す。
     * @return 見つかった Path。見つからなければ null
     */
    private static Path locateWhisperCli() {
        Path p = locateTool("whisper-cli.exe");
        return (p != null) ? p : locateTool("main.exe");
    }

    /**
     * ggml-*.bin モデルを探す。base を優先、無ければサイズ違いも許容。
     * @return 見つかった Path。見つからなければ null
     */
    private static Path locateModel() {
        for (Path base : modelDirCandidates()) {
            for (String name : new String[]{
                    "ggml-base.bin", "ggml-small.bin", "ggml-medium.bin",
                    "ggml-tiny.bin", "ggml-large.bin"
            }) {
                Path p = base.resolve(name);
                if (Files.exists(p)) return p.toAbsolutePath();
            }
        }
        return null;
    }

    /**
     * lib/tools の候補パス一覧。実行ディレクトリ違いに対応。
     * @return パスの配列
     */
    private static Path[] toolsDirCandidates() {
        return new Path[]{
                Paths.get("lib/tools"),
                Paths.get("CookingRecipe/lib/tools"),
                Paths.get("../lib/tools"),
                Paths.get("../../lib/tools")
        };
    }

    /**
     * lib/models の候補パス一覧。
     * @return パスの配列
     */
    private static Path[] modelDirCandidates() {
        return new Path[]{
                Paths.get("lib/models"),
                Paths.get("CookingRecipe/lib/models"),
                Paths.get("../lib/models"),
                Paths.get("../../lib/models")
        };
    }

    /**
     * 一時ディレクトリ配下を再帰的に削除する(失敗は黙殺)。
     * @param root 削除対象のルートパス
     */
    private static void deleteRecursively(Path root) {
        try {
            Files.walk(root)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignore) {} });
        } catch (IOException ignore) {}
    }
}
