package main.java.UI;

import java.awt.Color;
import java.awt.Font;

/**
 * UIで使用する色とフォントの定数を集約するクラス
 */
public final class Theme {
    public static final Color COLOR_PRIMARY = new Color(52, 152, 219);
    public static final Color COLOR_BG = new Color(245, 247, 250);
    public static final Color COLOR_SIDE = new Color(44, 62, 80);
    public static final Color COLOR_TEXT_LIGHT = Color.WHITE;
    public static final Color COLOR_DELETE = new Color(231, 76, 60);
    public static final Color COLOR_AI = new Color(155, 89, 182);

    public static final Font FONT_MAIN = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);

    private Theme() {}
}
