import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.stream.Collectors;

/**
 * 各画面で共通利用するSwingコンポーネントの生成ヘルパー
 */
public final class UIComponents {

    public static JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 35));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(Theme.FONT_MAIN);
        return field;
    }

    public static JList<Recipe> createStyledRecipeList(DefaultListModel<Recipe> model) {
        JList<Recipe> list = new JList<>(model);
        list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getTitle());
            label.setOpaque(true);
            label.setFont(Theme.FONT_MAIN);
            label.setBorder(new EmptyBorder(8, 15, 8, 15));
            if (isSelected) {
                label.setBackground(Theme.COLOR_PRIMARY);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
            }
            return label;
        });
        list.setPreferredSize(new Dimension(200, 0));
        return list;
    }

    public static JTextArea createDetailArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Theme.FONT_MAIN);
        area.setMargin(new Insets(20, 20, 20, 20));
        return area;
    }

    public static void updateDetailArea(JTextArea area, Recipe recipe) {
        String ings = recipe.getIngredients().stream()
                .map(Ingredient::getName)
                .collect(Collectors.joining(", "));
        String categories = recipe.getCategories().stream()
                .map(RecipeCategory::getDisplayName)
                .collect(Collectors.joining(", "));
        area.setText("【レシピ名】\n" + recipe.getTitle()
                + "\n\n【カテゴリー】\n" + categories
                + "\n\n【URL】\n" + recipe.getUrl()
                + "\n\n【必要食材】\n" + ings);
    }

    public static void addLeftAligned(JPanel panel, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(comp);
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Theme.COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        return btn;
    }

    private UIComponents() {}
}
