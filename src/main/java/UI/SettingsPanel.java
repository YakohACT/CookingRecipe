package main.java.UI;

import main.java.SwingMain;
import main.java.AI.RecipeAIService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * AI利用設定画面パネル。
 * APIキーは ChatGPT / Gemini / Claude それぞれで個別に保存され、
 * プロバイダーを切替えると該当プロバイダーの保存済みキーが復元される。
 */
public class SettingsPanel extends JPanel {

    /**
     * AI設定パネルを構築する。プロバイダー切替で該当プロバイダーの保存済みモデル/キーを復元。
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
        modelCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        UIComponents.addLeftAligned(form, modelCombo);

        form.add(Box.createRigidArea(new Dimension(0, 15)));

        UIComponents.addLeftAligned(form, new JLabel("APIキー (プロバイダーごとに別々に保存されます):"));
        JPasswordField keyField = new JPasswordField();
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        UIComponents.attachContextMenu(keyField);
        UIComponents.addLeftAligned(form, keyField);

        // プロバイダー切替時に、そのプロバイダーで保存済みのモデル/APIキーを再読み込み
        Runnable refreshForProvider = () -> {
            RecipeAIService.Provider p = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            updateModelCombo(p, modelCombo);
            if (p != null) {
                String savedModel = owner.getModelFor(p);
                if (!savedModel.isEmpty()) modelCombo.setSelectedItem(savedModel);
                keyField.setText(owner.getApiKeyFor(p));
            } else {
                keyField.setText("");
            }
        };
        providerCombo.addActionListener(e -> refreshForProvider.run());
        refreshForProvider.run();

        form.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton btnSave = UIComponents.createPrimaryButton("設定を保存");
        btnSave.addActionListener(e -> {
            RecipeAIService.Provider selected = (RecipeAIService.Provider) providerCombo.getSelectedItem();
            owner.setCurrentAiProvider(selected);
            owner.setModelFor(selected, (String) modelCombo.getSelectedItem());
            owner.setApiKeyFor(selected, new String(keyField.getPassword()).trim());
            JOptionPane.showMessageDialog(this, "設定を保存しました");
            owner.showWelcome();
        });
        UIComponents.addLeftAligned(form, btnSave);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    /**
     * 指定プロバイダーが対応するモデル一覧をモデル用コンボボックスに反映する。
     * @param provider   対象のAIプロバイダー(null可)
     * @param modelCombo 反映先のコンボボックス
     */
    private void updateModelCombo(RecipeAIService.Provider provider, JComboBox<String> modelCombo) {
        modelCombo.removeAllItems();
        if (provider == null) return;
        String[] availableModels = new RecipeAIService().getModelsForProvider(provider);
        for (String model : availableModels) {
            modelCombo.addItem(model);
        }
    }
}
