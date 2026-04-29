import java.io.*;
import java.util.ArrayList;

/**
 * 全レシピの保持とファイル永続化を管理するクラス
 */
public class AllRecipeList implements Serializable {
    private ArrayList<Recipe> recipeList;
    private final String FILE_NAME = "recipes.dat";

    public AllRecipeList() {
        recipeList = new ArrayList<>();
        read();
    }

    public ArrayList<Recipe> getRecipeList() {
        return recipeList;
    }

    public void addRecipe(Recipe recipe) {
        recipeList.add(recipe);
    }

    /**
     * 指定されたインデックスのレシピを削除
     * @param id 削除するレシピのインデックス
     */
    public void deleteRecipe(int id) {
        if (id >= 0 && id < recipeList.size()) {
            recipeList.remove(id);
        }
    }

    /**
     * 指定されたレシピオブジェクトを直接削除
     * @param recipe 削除するレシピオブジェクト
     */
    public void deleteRecipe(Recipe recipe) {
        recipeList.remove(recipe);
    }

    /**
     * 指定されたインデックスのレシピを変更
     * @param id 変更するレシピのインデックス
     * @param recipe 変更後のレシピオブジェクト
     */
    public void changeRecipe(int id, Recipe recipe) {
        if (id >= 0 && id < recipeList.size()) {
            recipeList.set(id, recipe);
        }
    }

    /**
     * ファイルからのレシピデータ読み込み
     */
    @SuppressWarnings("unchecked")
    public void read() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            recipeList = (ArrayList<Recipe>) ois.readObject();
        } catch (InvalidClassException e) {
            // クラス定義変更後の互換性エラー。空リストで起動する。
            System.err.println("[警告] 保存データの形式が変わったため読み込めませんでした。新規データで起動します。");
            recipeList = new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[エラー] レシピデータの読み込みに失敗しました: " + e.getMessage());
            e.printStackTrace();
            recipeList = new ArrayList<>();
        }
    }

    /**
     * ファイルへのレシピデータ保存
     */
    public void write() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(recipeList);
        } catch (Exception e) {
            System.err.println("[エラー] レシピデータの保存に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
