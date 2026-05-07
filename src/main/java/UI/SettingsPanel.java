package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeAIService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;

/**
 * AI利用設定画面パネル。
 * APIキーは OpenAI / Gemini / Claude / Ollama それぞれで個別に保存され、
 * プロバイダーを切替えると該当プロバイダーの保存済みキーが復元される。
 *
 * モデル一覧は次のタイミングで API から動的取得する:
 *   - プロバイダー切替時
 *   - APIキーフィールドからフォーカスが外れた時(入力直後)
 *   - 「更新」ボタン押下時
 * 取得失敗時は各プロバイダーの静的フォールバックリストにフォールバックする。
 */
public class SettingsPanel extends JPanel {

    /**
     * AI設定パネルを構築する。プロバイダー切替で該当プロバイダーの保存済みモデル/キーを復元し、
     * モデル一覧は API から非同期で動的取得する。
     * @param owner 共有状態(プロバイダー、モデル、APIキー等)を保持するフレーム
     */
    public SettingsPanel(SwingMain owner) {
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("AI利用設定");
        titleLabel.setFont(Theme.FONT_TITLE);
        UIComponents.addLeftAligned(form, titleLabel);
        form.add(Box.createRigidArea(new Dimension(0, 20)));

        UIComponents.addLeftAligned(form, new JLabel("利用するAIプロバイダー:"));
        JComboBox<RecipeAIService.Provider> providerCombo = new JComboBox<>(RecipeAIService.Provider.values());
        providerCombo.setSelectedItem(owner.getCurrentAiProvider());
        providerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        UIComponents.addLeftAligned(form, providerCombo);
        form.add(Box.createRigidArea(new Dimension(0, 15)));

        UIComponents.addLeftAligned(form, new JLabel("利用するモデル:"));
        JComboBox<String> modelCombo = new JComboBox<>();
        JButton refreshBtn = new JButton("更新");
        refreshBtn.setToolTipText("APIに問い合わせてモデル一覧を再取得");
        // モデルコンボと更新ボタンを横並びにする
        JPanel modelRow = new JPanel(new BorderLayout(8, 0));
        modelRow.setOpaque(false);
        modelRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        modelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        modelRow.add(modelCombo, BorderLayout.CENTER);
        modelRow.add(refreshBtn, BorderLayout.EAST);
        form.add(modelRow);

        // APIキー欄は Ollama 選択時に非表示にしたいので、ラベル+上の余白+入力欄を
        // 1つのコンテナにまとめて setVisible で一括制御できるようにする
        JPanel keySection = new JPanel();
        keySection.setLayout(new BoxLayout(keySection, BoxLayout.Y_AXIS));
        keySection.setOpaque(false);
        keySection.setAlignmentX(Component.LEFT_ALIGNMENT);
        keySection.add(Box.createRigidArea(new Dimension(0, 15)));
        JLabel keyLabel = new JLabel("APIキー (プロバイダーごとに別々に保存されます):");
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        keySection.add(keyLabel);
        JPasswordField keyField = new JPasswordField();
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        keyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        UIComponents.attachContextMenu(keyField);
        keySection.add(keyField);
        form.add(keySection);

        // ===== モデル一覧の動的取得 =====
        // ステップ1: 静的フォールバックを即時表示し、UIに何か出た状態にする
        // ステップ2: SwingWorkerで API を叩き、結果が帰ったらコンボの中身を差し替える
        Runnable populateModels = () -> {
            RecipeAIService.Provider p = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            if (p == null) {
                modelCombo.removeAllItems();
                return;
            }
            // ステップ1: 静的リストを先に出す
            RecipeAIService service = new RecipeAIService();
            modelCombo.removeAllItems();
            for (String m : service.getModelsForProvider(p)) modelCombo.addItem(m);
            String saved = owner.getModelFor(p);
            if (saved != null && !saved.isEmpty()) modelCombo.setSelectedItem(saved);

            // ステップ2: 非同期に API から取得
            String currentKey = new String(keyField.getPassword()).trim();
            refreshBtn.setEnabled(false);
            String previousLabel = refreshBtn.getText();
            refreshBtn.setText("取得中...");
            SwingWorker<String[], Void> w = new SwingWorker<>() {
                @Override
                protected String[] doInBackground() {
                    return service.fetchModelsForProvider(p, currentKey);
                }
                @Override
                protected void done() {
                    refreshBtn.setText(previousLabel);
                    refreshBtn.setEnabled(true);
                    try {
                        String[] models = get();
                        if (models == null || models.length == 0) return;
                        // 現在の選択を維持できるなら維持する
                        String currentSel = (String) modelCombo.getSelectedItem();
                        modelCombo.removeAllItems();
                        for (String m : models) modelCombo.addItem(m);
                        if (currentSel != null && Arrays.asList(models).contains(currentSel)) {
                            modelCombo.setSelectedItem(currentSel);
                        } else {
                            String savedModel = owner.getModelFor(p);
                            if (savedModel != null && Arrays.asList(models).contains(savedModel)) {
                                modelCombo.setSelectedItem(savedModel);
                            }
                        }
                    } catch (Exception ignore) { /* 既に静的フォールバックが入っている */ }
                }
            };
            w.execute();
        };

        // Ollama はAPIキー不要なので、選択時はキー欄を丸ごと非表示にする
        Runnable applyKeyVisibility = () -> {
            RecipeAIService.Provider p = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            boolean needsKey = (p != null && p != RecipeAIService.Provider.OLLAMA);
            keySection.setVisible(needsKey);
            keySection.revalidate();
            keySection.repaint();
        };

        // プロバイダー切替時にAPIキー欄の表示/中身を更新し、モデル一覧も再取得
        providerCombo.addActionListener(e -> {
            RecipeAIService.Provider p = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            keyField.setText(p == null ? "" : owner.getApiKeyFor(p));
            applyKeyVisibility.run();
            populateModels.run();
        });

        // APIキー入力後フォーカスが外れたタイミングでモデル一覧を更新
        keyField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                populateModels.run();
            }
        });

        // 更新ボタン
        refreshBtn.addActionListener(e -> populateModels.run());

        // 初期表示
        RecipeAIService.Provider initial = (RecipeAIService.Provider) providerCombo.getSelectedItem();
        if (initial != null) keyField.setText(owner.getApiKeyFor(initial));
        applyKeyVisibility.run();
        populateModels.run();

        form.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton btnSave = UIComponents.createPrimaryButton("設定を保存");
        btnSave.addActionListener(e -> {
            RecipeAIService.Provider selected = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            owner.setCurrentAiProvider(selected);
            owner.setModelFor(selected, (String) modelCombo.getSelectedItem());
            // Ollama はキー不要なので保存スキップ
            if (selected != RecipeAIService.Provider.OLLAMA) {
                owner.setApiKeyFor(selected, new String(keyField.getPassword()).trim());
            }
            JOptionPane.showMessageDialog(this, "設定を保存しました");
            owner.showWelcome();
        });
        UIComponents.addLeftAligned(form, btnSave);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }
}
