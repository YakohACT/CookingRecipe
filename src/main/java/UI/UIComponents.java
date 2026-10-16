package main.java.UI;

import main.java.AI.AiSettings;
import main.java.Recipe.Ingredient;
import main.java.Recipe.Quantity;
import main.java.Recipe.Recipe;
import main.java.Recipe.RecipeCategory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * 各画面で共通利用するSwingコンポーネントの生成ヘルパー
 */
public final class UIComponents {

    /** スクロール1単位あたりの移動量(px)。ホイール1ノッチは通常3単位分動く */
    private static final int SCROLL_UNIT_INCREMENT = 16;

    /**
     * 高さ35px・テーマフォントを設定済みの JTextField を生成する。
     * 右クリックメニューも自動的に取り付けられる。
     * @return スタイル適用済みの JTextField
     */
    public static JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(0, 35));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(Theme.FONT_MAIN);
        attachContextMenu(field);
        return field;
    }

    /**
     * 任意の JTextComponent (JTextField/JTextArea/JEditorPane/JPasswordField) に
     * 右クリックの「切り取り/コピー/貼り付け/すべて選択」メニューを付ける。
     * 読み取り専用コンポーネントでは切り取り/貼り付けが自動的に無効化される。
     * @param comp メニューを取り付けたいテキスト系コンポーネント
     */
    public static void attachContextMenu(JTextComponent comp) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem cut = new JMenuItem("切り取り");
        cut.addActionListener(e -> comp.cut());

        JMenuItem copy = new JMenuItem("コピー");
        copy.addActionListener(e -> comp.copy());

        JMenuItem paste = new JMenuItem("貼り付け");
        paste.addActionListener(e -> comp.paste());

        JMenuItem selectAll = new JMenuItem("すべて選択");
        selectAll.addActionListener(e -> comp.selectAll());

        menu.add(cut);
        menu.add(copy);
        menu.add(paste);
        menu.addSeparator();
        menu.add(selectAll);

        // 表示直前に各項目の有効/無効を更新する
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean editable = comp.isEditable() && comp.isEnabled();
                boolean hasSelection = comp.getSelectedText() != null && !comp.getSelectedText().isEmpty();
                boolean clipboardHasText;
                try {
                    clipboardHasText = Toolkit.getDefaultToolkit().getSystemClipboard()
                            .isDataFlavorAvailable(DataFlavor.stringFlavor);
                } catch (Exception ex) {
                    clipboardHasText = false;
                }
                cut.setEnabled(editable && hasSelection);
                copy.setEnabled(hasSelection);
                paste.setEnabled(editable && clipboardHasText);
                selectAll.setEnabled(comp.getDocument().getLength() > 0);
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        comp.setComponentPopupMenu(menu);
    }

    /**
     * レシピ一覧表示用の、テーマ追従カラーを持つ JList を生成する。
     * @param model 表示する DefaultListModel
     * @return スタイル適用済みの JList
     */
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
        // 幅だけを固定する。setPreferredSize で高さまで固定すると、件数が多くても
        // スクロールペイン内で一覧が伸びず、表示しきれないレシピまでスクロールできなくなる
        list.setFixedCellWidth(200);
        return list;
    }

    /**
     * フォームなどの通常パネルを包むスクロールペインを生成する。
     * JPanel のように行の概念を持たない部品はスクロール量の既定値が1pxで、
     * マウスホイール1ノッチで数ピクセルしか動かないため、移動量を設定する。
     * @param view スクロールさせるパネル
     * @return スクロール量を設定済みの JScrollPane
     */
    public static JScrollPane createScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        return scrollPane;
    }

    /**
     * レシピ詳細表示用の HTML 対応エリアを生成する。
     * 中の URL はハイパーリンクとしてクリック可能で、デフォルトブラウザで開く。
     * 右クリックメニュー(コピー等)も付与済み。
     * @return 詳細表示用 JEditorPane
     */
    public static JEditorPane createDetailArea() {
        JEditorPane area = new JEditorPane();
        area.setContentType("text/html");
        area.setEditable(false);
        area.setMargin(new Insets(20, 20, 20, 20));
        area.setFont(Theme.FONT_MAIN);
        attachContextMenu(area);
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

    /**
     * 詳細エリアの内容を指定レシピで再描画する。
     * テーマ追従の色とハイパーリンクを HTML で組み立てる。
     * @param area   表示先の JEditorPane
     * @param recipe 表示するレシピ
     */
    public static void updateDetailArea(JEditorPane area, Recipe recipe) {
        // 食材は1行に1つ、分量があれば「・鶏もも　200g」のように並べる
        StringBuilder ings = new StringBuilder();
        for (Ingredient ing : recipe.getIngredients()) {
            Quantity q = recipe.getQuantity(ing);
            if (ings.length() > 0) ings.append("<br>");
            ings.append("・").append(escapeHtml(ing.getName()));
            if (!q.isEmpty()) ings.append("　").append(escapeHtml(q.toString()));
        }
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
                + "<b>【必要食材】</b><br>" + ings
                + "</body></html>";
        area.setText(html);
        area.setCaretPosition(0);
    }

    /**
     * HTMLテキスト埋め込み用に &amp;/&lt;/&gt; をエスケープする。
     * @param s 任意の文字列(null可)
     * @return エスケープ後の文字列
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * HTML属性値用に追加で {@code "} をエスケープする。
     * @param s 任意の文字列(null可)
     * @return エスケープ後の文字列
     */
    private static String escapeAttr(String s) {
        if (s == null) return "";
        return escapeHtml(s).replace("\"", "&quot;");
    }

    /**
     * 色をHTML/CSS用の #RRGGBB 形式に変換する。
     * @param c 変換する色
     * @return "#xxxxxx" 形式の文字列
     */
    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * BoxLayout 配下のコンポーネントを左寄せして親に追加する。
     * @param panel 追加先パネル
     * @param comp  追加するコンポーネント
     */
    public static void addLeftAligned(JPanel panel, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(comp);
    }

    /**
     * テーマのプライマリ色を背景にした強調ボタンを生成する。
     * @param text ボタンに表示するラベル
     * @return スタイル適用済みの JButton
     */
    public static JButton createPrimaryButton(String text) {
        return createColoredButton(text, Theme.COLOR_PRIMARY);
    }

    /**
     * 指定色を背景にした白文字のボタンを生成する(削除ボタンなど)。
     * @param text       ボタンに表示するラベル
     * @param background 背景色
     * @return スタイル適用済みの JButton
     */
    public static JButton createColoredButton(String text, Color background) {
        JButton btn = new JButton(text);
        btn.setBackground(background);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        return btn;
    }

    /**
     * AI機能を呼び出すボタン(紫色・ハンドカーソル)を生成する。
     * @param text ボタンに表示するラベル
     * @return スタイル適用済みの JButton
     */
    public static JButton createAiButton(String text) {
        JButton btn = createColoredButton(text, Theme.COLOR_AI);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * AIを呼び出せる設定になっているかを確認し、未設定なら警告ダイアログを出す。
     * @param parent ダイアログの親コンポーネント
     * @param ai     AI利用設定
     * @return 呼び出し可能なら true
     */
    public static boolean ensureAiReady(Component parent, AiSettings ai) {
        if (ai.isReady()) return true;
        JOptionPane.showMessageDialog(parent,
                "先に「AI設定」メニューでプロバイダーとAPIキー(llama.cpp の場合はサーバーURL)を設定してください。",
                "AI未設定", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private UIComponents() {}
}
