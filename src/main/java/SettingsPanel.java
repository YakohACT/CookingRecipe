import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * AI利用設定画面パネル
 */
public class SettingsPanel extends JPanel {

    public SettingsPanel(SwingMain owner) {
        setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
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

        providerCombo.addActionListener(e -> updateModelCombo(providerCombo, modelCombo));
        updateModelCombo(providerCombo, modelCombo);
        modelCombo.setSelectedItem(owner.getCurrentModelName());

        form.add(Box.createRigidArea(new Dimension(0, 15)));

        UIComponents.addLeftAligned(form, new JLabel("APIキー:"));
        JPasswordField keyField = new JPasswordField(owner.getCurrentApiKey());
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        UIComponents.addLeftAligned(form, keyField);

        form.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton btnSave = UIComponents.createPrimaryButton("設定を保存");
        btnSave.addActionListener(e -> {
            owner.setCurrentAiProvider((RecipeAIService.Provider) providerCombo.getSelectedItem());
            owner.setCurrentModelName((String) modelCombo.getSelectedItem());
            owner.setCurrentApiKey(new String(keyField.getPassword()).trim());
            JOptionPane.showMessageDialog(this, "設定を保存しました");
            owner.showWelcome();
        });
        UIComponents.addLeftAligned(form, btnSave);

        add(new JScrollPane(form), BorderLayout.CENTER);
    }

    /**
     * 選択中のプロバイダーが対応するモデル一覧をモデル用コンボボックスに反映する
     */
    private void updateModelCombo(JComboBox<RecipeAIService.Provider> providerCombo, JComboBox<String> modelCombo) {
        modelCombo.removeAllItems();
        RecipeAIService.Provider selectedProvider = (RecipeAIService.Provider) providerCombo.getSelectedItem();

        RecipeAIService service = new RecipeAIService();
        String[] availableModels = service.getModelsForProvider(selectedProvider);
        for (String model : availableModels) {
            modelCombo.addItem(model);
        }
    }
}
