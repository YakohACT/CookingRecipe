package main.java.UI;

import main.java.SwingMain;
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
