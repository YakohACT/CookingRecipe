package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeAIService;
import main.java.Recipe.BatchUrlImporter;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 保存済みレシピをJSONファイルへ書き出す/読み込む画面。
 * バックアップや別端末への移行に利用する。
 */
public class DataIOPanel extends JPanel {

    /**
     * データ入出力パネルを構築する。
     * @param owner 共有状態(レシピリスト・食材マスタ)にアクセスするためのフレーム参照
     */
    public DataIOPanel(SwingMain owner) {
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("データ入出力");
        titleLabel.setFont(Theme.FONT_TITLE);
        UIComponents.addLeftAligned(form, titleLabel);
        form.add(Box.createRigidArea(new Dimension(0, 24)));

        // ===== エクスポート =====
        JLabel expHeader = new JLabel("エクスポート");
        expHeader.setFont(Theme.FONT_TITLE.deriveFont(15f));
        UIComponents.addLeftAligned(form, expHeader);
        UIComponents.addLeftAligned(form, wrapText(
                "現在保存されている全レシピを JSON ファイルに書き出します。"
                + "別端末への移行やバックアップに利用できます。"));
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnExport = UIComponents.createPrimaryButton("レシピをファイルに書き出す…");
        btnExport.addActionListener(e -> doExport(owner));
        UIComponents.addLeftAligned(form, btnExport);

        form.add(Box.createRigidArea(new Dimension(0, 32)));

        // ===== インポート =====
        JLabel impHeader = new JLabel("インポート");
        impHeader.setFont(Theme.FONT_TITLE.deriveFont(15f));
        UIComponents.addLeftAligned(form, impHeader);
        UIComponents.addLeftAligned(form, wrapText(
                "JSON ファイルからレシピを読み込み、現在のデータベースに追加します。"
                + "既存のレシピは削除されません(同名レシピも追加されます)。"));
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnImport = UIComponents.createPrimaryButton("ファイルからレシピを読み込む…");
        btnImport.addActionListener(e -> doImport(owner));
        UIComponents.addLeftAligned(form, btnImport);

        form.add(Box.createRigidArea(new Dimension(0, 32)));

        // ===== URLバッチ取込み (AI) =====
        JLabel batchHeader = new JLabel("URLバッチ取込み (AI)");
        batchHeader.setFont(Theme.FONT_TITLE.deriveFont(15f));
        UIComponents.addLeftAligned(form, batchHeader);
        UIComponents.addLeftAligned(form, wrapText(
                "1行に1つの URL を記述したテキスト(.txt)ファイルを選び、各 URL を"
                + " 現在選択中の AI プロバイダー(設定画面で選択中のもの)で順次解析して"
                + " 一括登録します。空行と <code>#</code> から始まるコメント行は無視されます。"
                + " カテゴリーは「その他」固定で登録されるため、必要に応じて後で編集してください。"));
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnBatch = UIComponents.createPrimaryButton("URLリストから一括登録…");
        btnBatch.addActionListener(e -> doBatchUrlImport(owner));
        UIComponents.addLeftAligned(form, btnBatch);

        form.add(Box.createRigidArea(new Dimension(0, 32)));

        // ===== 現状サマリー =====
        int count = owner.getAllRecipeList().getRecipeList().size();
        JLabel summary = new JLabel("現在登録されているレシピ: " + count + "件");
        summary.setFont(Theme.FONT_MAIN);
        UIComponents.addLeftAligned(form, summary);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    /**
     * 説明文用のラベルを生成する(左寄せ・テーマフォント)。
     * @param text 表示する文字列
     * @return 設定済みの JLabel
     */
    private JComponent wrapText(String text) {
        JLabel l = new JLabel("<html><body style='width:480px'>" + text + "</body></html>");
        l.setFont(Theme.FONT_MAIN);
        return l;
    }

    /**
     * エクスポート処理: ファイル選択 → JSON書き出し。
     * @param owner 共有状態を持つフレーム
     */
    private void doExport(SwingMain owner) {
        List<Recipe> recipes = owner.getAllRecipeList().getRecipeList();
        if (recipes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "エクスポートできるレシピがありません。", "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("レシピのエクスポート先を選択");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON ファイル (*.json)", "json"));
        chooser.setSelectedFile(new File("recipes-export.json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        if (file.exists()) {
            int ow = JOptionPane.showConfirmDialog(this,
                    "ファイルが既に存在します。上書きしますか?\n" + file.getAbsolutePath(),
                    "上書き確認", JOptionPane.YES_NO_OPTION);
            if (ow != JOptionPane.YES_OPTION) return;
        }

        try {
            String json = RecipeIO.exportToJson(recipes);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this,
                    recipes.size() + "件のレシピを書き出しました。\n" + file.getAbsolutePath(),
                    "エクスポート完了", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "書き出しに失敗しました: " + ex.getMessage(),
                    "エラー", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * URLバッチ取込み: テキストファイル選択 → 各URLをAIに解析させて順次登録。
     * AI呼び出しは時間がかかるためモーダル進捗ダイアログを表示し、キャンセル可能にする。
     * @param owner 共有状態を持つフレーム
     */
    private void doBatchUrlImport(SwingMain owner) {
        // 1. AI 設定チェック (Ollama 以外は APIキー必須)
        if (owner.getCurrentAiProvider() != RecipeAIService.Provider.OLLAMA
                && owner.getCurrentApiKey().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "先に「AI設定」メニューでプロバイダーとAPIキーを設定してください。",
                    "AI未設定", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. ファイル選択
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("URLリストの.txtファイルを選択");
        chooser.setFileFilter(new FileNameExtensionFilter("テキストファイル (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        // 3. URL 読み込み
        final List<String> urls;
        try {
            urls = BatchUrlImporter.readUrls(file.toPath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "ファイル読み込みに失敗しました: " + ex.getMessage(),
                    "エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (urls.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "ファイル内に有効なURLが見つかりませんでした。",
                    "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 4. 確認
        int confirm = JOptionPane.showConfirmDialog(this,
                urls.size() + " 件のURLをAIで解析して一括登録します。\n"
                + "(1件あたり10〜60秒程度かかります)\n\n続行しますか？",
                "一括登録の確認", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) return;

        // 5. 進捗ダイアログ + バックグラウンド処理
        runBatchImport(owner, urls);
    }

    /**
     * モーダル進捗ダイアログを表示しつつ {@link BatchUrlImporter#importAll} を実行する。
     * @param owner 共有状態
     * @param urls  処理対象URLリスト
     */
    private void runBatchImport(SwingMain owner, List<String> urls) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "AIで一括登録中…", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel statusLabel = new JLabel("準備中...");
        statusLabel.setFont(Theme.FONT_MAIN);
        JProgressBar bar = new JProgressBar(0, urls.size());
        bar.setStringPainted(true);
        JButton cancelBtn = new JButton("キャンセル");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        UIComponents.addLeftAligned(content, statusLabel);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(bar);
        content.add(Box.createRigidArea(new Dimension(0, 16)));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(cancelBtn);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(btnRow);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setSize(Math.max(480, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(this);

        // ユーザーがキャンセルしたかを SwingWorker / バックグラウンド側に伝えるフラグ
        final boolean[] cancelled = {false};

        SwingWorker<BatchUrlImporter.Result, BatchUrlImporter.Progress> worker = new SwingWorker<>() {
            @Override
            protected BatchUrlImporter.Result doInBackground() {
                RecipeAIService aiService = new RecipeAIService();
                aiService.setConfig(owner.getCurrentAiProvider(),
                        owner.getCurrentApiKey(),
                        owner.getCurrentModelName());
                return BatchUrlImporter.importAll(urls,
                        aiService,
                        owner.getIngredientMaster(),
                        owner.getAllRecipeList(),
                        this::publish,
                        () -> cancelled[0]);
            }

            @Override
            protected void process(List<BatchUrlImporter.Progress> chunks) {
                BatchUrlImporter.Progress last = chunks.get(chunks.size() - 1);
                // 進捗バー: PROCESSING ならカレントの開始位置、それ以外(成功/失敗/スキップ)は1件分加算
                int progressed = last.index + (last.status == BatchUrlImporter.Status.PROCESSING ? 0 : 1);
                bar.setValue(progressed);
                String stage;
                switch (last.status) {
                    case PROCESSING:        stage = "解析中"; break;
                    case SUCCEEDED:         stage = "登録完了"; break;
                    case SKIPPED_DUPLICATE: stage = "スキップ(URL重複)"; break;
                    case FAILED:
                    default:                stage = "失敗"; break;
                }
                statusLabel.setText("[" + (last.index + 1) + "/" + last.total + "] " + stage + ": " + last.url);
            }

            @Override
            protected void done() {
                dialog.dispose();
                BatchUrlImporter.Result r;
                try {
                    r = get();
                } catch (Exception ignore) {
                    r = new BatchUrlImporter.Result(0, 0, 0, 0);
                }
                String msg = (cancelled[0] ? "キャンセルされました。\n" : "一括登録が完了しました。\n")
                        + "成功: " + r.succeeded + "件\n"
                        + "失敗: " + r.failed + "件\n"
                        + "スキップ(URL重複): " + r.skipped + "件";
                JOptionPane.showMessageDialog(DataIOPanel.this, msg, "結果",
                        JOptionPane.INFORMATION_MESSAGE);
                owner.showWelcome();
            }
        };

        cancelBtn.addActionListener(e -> {
            cancelled[0] = true;
            cancelBtn.setEnabled(false);
            statusLabel.setText("キャンセル中... (現在のリクエスト完了を待機)");
        });

        worker.execute();
        dialog.setVisible(true); // モーダル: dispose() されるまで EDT のサブイベントループで処理継続
    }

    /**
     * インポート処理: ファイル選択 → JSON読み込み → AllRecipeList に追加。
     * @param owner 共有状態を持つフレーム
     */
    private void doImport(SwingMain owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("読み込むレシピJSONファイルを選択");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON ファイル (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            List<Recipe> imported = RecipeIO.parseFromJson(json, owner.getIngredientMaster());
            if (imported.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "ファイルから有効なレシピが見つかりませんでした。",
                        "情報", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    imported.size() + "件のレシピが見つかりました。追加しますか?",
                    "インポート確認", JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION) return;

            int added = 0;
            for (Recipe r : imported) {
                owner.getAllRecipeList().addRecipe(r);
                added++;
            }
            JOptionPane.showMessageDialog(this,
                    added + "件のレシピを追加しました。",
                    "インポート完了", JOptionPane.INFORMATION_MESSAGE);
            owner.showWelcome();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "読み込みに失敗しました: " + ex.getMessage(),
                    "エラー", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
