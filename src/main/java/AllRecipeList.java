import java.util.ArrayList;

/**
 * 全レシピをメモリに保持し、SQLite と同期させるクラス。
 * add/delete は即時SQLに反映するため write() の呼び出しは不要(no-op として残置)。
 */
public class AllRecipeList {

    private final RecipeRepository repository;
    private ArrayList<Recipe> recipeList;

    public AllRecipeList(IngredientMaster ingredientMaster) {
        this.repository = new RecipeRepository(RecipeRepository.resolveDbPath(), ingredientMaster);
        try {
            this.recipeList = new ArrayList<>(repository.findAll());
        } catch (Exception e) {
            System.err.println("[エラー] レシピの読み込みに失敗しました: " + e.getMessage());
            e.printStackTrace();
            this.recipeList = new ArrayList<>();
        }
    }

    public ArrayList<Recipe> getRecipeList() {
        return recipeList;
    }

    public void addRecipe(Recipe recipe) {
        long id = repository.insert(recipe);
        recipe.setId(id);
        recipeList.add(recipe);
    }

    public void deleteRecipe(Recipe recipe) {
        if (recipe.getId() != 0) {
            repository.delete(recipe.getId());
        }
        recipeList.remove(recipe);
    }

    /**
     * 既存レシピを更新する。SQLiteと同期し、メモリ上のリストの該当エントリも差し替える
     */
    public void updateRecipe(Recipe recipe) {
        repository.update(recipe);
        for (int i = 0; i < recipeList.size(); i++) {
            if (recipeList.get(i).getId() == recipe.getId()) {
                recipeList.set(i, recipe);
                return;
            }
        }
    }

    /** SQLite移行に伴い不要になったが、既存呼び出し(終了時など)を壊さないよう no-op として残す */
    public void write() {}

    /** 旧API互換のno-op。実体はコンストラクタで読み込み済み */
    public void read() {}
}
