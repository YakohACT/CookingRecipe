import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 各AIプロバイダーの共通処理をまとめた抽象クラス
 */
public abstract class AbstractRecipeAIProvider {

    /**
     * AIを利用してレシピを提案する
     *
     * @param apiKey         APIキー
     * @param modelName      利用するAIモデル名
     * @param url            参考にする動画やWebのURL
     * @param allIngredients 利用可能な食材リスト
     */
    public abstract String[] generateRecipe(String apiKey, String modelName, String url, ArrayList<Ingredient> allIngredients) throws Exception;

    protected String buildPrompt(String url, ArrayList<Ingredient> allIngredients) {
        String names = allIngredients.stream().map(Ingredient::getName).collect(Collectors.joining(","));
        String prompt = "優秀なシェフとして、以下の食材から3つ選びレシピを提案してください。";

        if (url != null && !url.trim().isEmpty()) {
            prompt += "特に、指定された動画(" + url + ")の内容（料理の雰囲気やジャンル）を大いに参考にして考案してください。";
        }

        return prompt + "形式厳守。タイトル:[名前]\\n食材:[A],[B]\\nこれ以外の回答は不要。\\n候補:" + names;
    }

    protected String[] parseStandardResponse(String text) {
        String title = "AI提案レシピ", ings = "";
        String formattedText = text.replace("\\n", "\n").replace("\\\"", "\"");

        for (String l : formattedText.split("\n")) {
            if (l.contains("タイトル:")) title = l.split(":")[1].trim();
            if (l.contains("食材:")) ings = l.split(":")[1].trim();
        }
        return new String[]{title, ings};
    }

    /**
     * このAIプロバイダーで利用可能なモデルの一覧を取得する
     *
     * @return モデル名の配列
     */
    public abstract String[] getAvailableModels();
}