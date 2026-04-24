import java.io.*;
import java.util.ArrayList;

/**
 * 登録されたすべてのレシピをリストとして管理するクラスです。
 * ファイルへの書き込み（保存）と読み込みを行い、データを永続化します。
 */
public class AllRecipeList implements Serializable {
    /** 登録されているすべてのレシピを保持するリスト */
    private ArrayList<Recipe> recipeList;
    /** 保存先となるファイルの名前 */
    private final String FILE_NAME = "recipes.dat";

    /**
     * コンストラクタ
     * リストを初期化した後、自動的にファイルから過去のデータを読み込む
     */
    public AllRecipeList() {
        recipeList = new ArrayList<>();
        read();
    }

    /**
     * 登録されているすべてのレシピのゲッター
     * * @return レシピのリスト
     */
    public ArrayList<Recipe> getRecipeList() {
        return recipeList;
    }

    /**
     * レシピリストに新しいレシピを追加する
     * * @param recipe 追加するレシピオブジェクト
     */
    public void addRecipe(Recipe recipe) {
        recipeList.add(recipe);
    }

    /**
     * 指定されたID（リストのインデックス番号）のレシピを削除する
     * * @param id 削除するレシピのインデックス番号（0始まり）
     */
    public void deleteRecipe(int id) {
        if (id >= 0 && id < recipeList.size()) {
            recipeList.remove(id);
        }
    }

    /**
     * 指定されたID（リストのインデックス番号）のレシピを新しいレシピに変更する
     * * @param id     変更対象のインデックス番号（0始まり）
     * @param recipe 変更後の新しいレシピオブジェクト
     */
    public void changeRecipe(int id, Recipe recipe) {
        if (id >= 0 && id < recipeList.size()) {
            recipeList.set(id, recipe);
        }
    }

    /**
     * serializeを利用して、設定されたファイル（recipes.dat）から
     * レシピリストのデータを読み込む
     */
    @SuppressWarnings("unchecked")
    public void read() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                recipeList = (ArrayList<Recipe>) ois.readObject();
            } catch (Exception e) {
                System.out.println("レシピデータの読み込みに失敗しました。");
            }
        }
    }

    /**
     * serializeを利用して、現在のレシピリストのデータを
     * 設定されたファイル（recipes.dat）に書き込みする
     */
    public void write() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(recipeList);
        } catch (Exception e) {
            System.out.println("レシピデータの保存に失敗しました。");
        }
    }
}