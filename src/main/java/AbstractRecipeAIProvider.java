import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 各AIプロバイダーの共通処理をまとめた抽象クラス
 */
public abstract class AbstractRecipeAIProvider {

    /**
     * AIを利用してレシピを提案する（各プロバイダーのクラスで実装）
     * @param apiKey APIキー
     * @param allIngredients 利用可能な食材リスト
     * @return [0]:タイトル, [1]:食材名（カンマ区切り）
     */
    public abstract String[] generateRecipe(String apiKey, ArrayList<Ingredient> allIngredients) throws Exception;

    /**
     * 共通のプロンプト（指示文）の作成
     */
    protected String buildPrompt(ArrayList<Ingredient> allIngredients) {
        String names = allIngredients.stream().map(Ingredient::getName).collect(Collectors.joining(","));
        return "優秀なシェフとして、以下の食材から3つ選びレシピを提案してください。" +
                "形式厳守。タイトル:[名前]\\n食材:[A],[B]\\nこれ以外の回答は不要。\\n候補:" + names;
    }

    /**
     * 抽出されたテキストからタイトルと食材を解析
     */
    protected String[] parseStandardResponse(String text) {
        String title = "AI提案レシピ", ings = "";
        String formattedText = text.replace("\\n", "\n").replace("\\\"", "\"");

        for (String l : formattedText.split("\n")) {
            if (l.contains("タイトル:")) title = l.split(":")[1].trim();
            if (l.contains("食材:")) ings = l.split(":")[1].trim();
        }
        return new String[]{title, ings};
    }
}