package main.java.UI;

import main.java.Recipe.Ingredient;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URI;
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
                // テーマ(Light/Dark)に追従するために UIManager から色を取得
                Color bg = UIManager.getColor("List.background");
                Color fg = UIManager.getColor("List.foreground");
                label.setBackground(bg != null ? bg : Color.WHITE);
                label.setForeground(fg != null ? fg : Color.BLACK);
            }
            return label;
        });
        list.setPreferredSize(new Dimension(200, 0));
        return list;
    }

    /**
     * レシピ詳細表示用の HTML 対応エリアを生成する。
     * 中の URL はハイパーリンクとしてクリック可能で、デフォルトブラウザで開く
     */
    public static JEditorPane createDetailArea() {
        JEditorPane area = new JEditorPane();
        area.setContentType("text/html");
        area.setEditable(false);
        area.setMargin(new Insets(20, 20, 20, 20));
        area.setFont(Theme.FONT_MAIN);
        area.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
            try {
                URI uri = (e.getURL() != null) ? e.getURL().toURI() : new URI(e.getDescription());
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(uri);
                        return;
                    }
                }
                JOptionPane.showMessageDialog(area,
                        "この環境ではブラウザを自動起動できません。\nURL: " + uri,
                        "情報", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(area,
                        "ブラウザの起動に失敗しました: " + ex.getMessage(),
                        "エラー", JOptionPane.ERROR_MESSAGE);
            }
        });
        return area;
    }

    public static void updateDetailArea(JEditorPane area, Recipe recipe) {
        String ings = recipe.getIngredients().stream()
                .map(Ingredient::getName)
                .collect(Collectors.joining(", "));
        String categories = recipe.getCategories().stream()
                .map(RecipeCategory::getDisplayName)
                .collect(Collectors.joining(", "));
        String url = recipe.getUrl() == null ? "" : recipe.getUrl();

        // テーマ追従の色を取得
        Color bg = UIManager.getColor("EditorPane.background");
        Color fg = UIManager.getColor("EditorPane.foreground");
        Color link = UIManager.getColor("Component.linkColor");
        if (link == null) link = Theme.COLOR_PRIMARY;

        StringBuilder bodyStyle = new StringBuilder("font-family: sans-serif; padding: 16px; line-height: 1.5;");
        if (bg != null) bodyStyle.append(" background-color: ").append(toHex(bg)).append(";");
        if (fg != null) bodyStyle.append(" color: ").append(toHex(fg)).append(";");

        String html = "<html><body style='" + bodyStyle + "'>"
                + "<b>【レシピ名】</b><br>" + escapeHtml(recipe.getTitle()) + "<br><br>"
                + "<b>【カテゴリー】</b><br>" + escapeHtml(categories) + "<br><br>"
                + "<b>【URL】</b><br>"
                + (url.isEmpty()
                    ? ""
                    : "<a href=\"" + escapeAttr(url) + "\" style=\"color: " + toHex(link) + ";\">"
                      + escapeHtml(url) + "</a>")
                + "<br><br>"
                + "<b>【必要食材】</b><br>" + escapeHtml(ings)
                + "</body></html>";
        area.setText(html);
        area.setCaretPosition(0);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String s) {
        if (s == null) return "";
        return escapeHtml(s).replace("\"", "&quot;");
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
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
