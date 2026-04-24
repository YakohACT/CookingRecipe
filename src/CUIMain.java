import java.util.ArrayList;
import java.util.Scanner;

/**
 * ターミナル（CUI）上でユーザーと対話するためのメインクラス
 * レシピの新規登録、削除、閲覧のメニューを提供し、各操作の入出力を管理する
 */
public class CUIMain {
    
    /**
     * プログラムの実行開始点（メインメソッド）
     * メインメニューの表示と、ユーザー入力に応じた処理の分岐を行う
     * * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AllRecipeList allRecipeList = new AllRecipeList();
        IngredientMaster ingredientMaster = new IngredientMaster();

        while (true) {
            System.out.println("\n=== レシピ管理システム ===");
            System.out.println("1. レシピを新規登録する");
            System.out.println("2. レシピを削除する");
            System.out.println("3. レシピを閲覧する");
            System.out.println("4. 保存して終了する");
            System.out.print("メニュー番号を入力してください: ");

            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    registerRecipe(scanner, allRecipeList, ingredientMaster);
                    break;
                case "2":
                    deleteRecipe(scanner, allRecipeList);
                    break;
                case "3":
                    viewMenu(scanner, allRecipeList, ingredientMaster);
                    break;
                case "4":
                    allRecipeList.write();
                    System.out.println("データを保存して終了します。お疲れ様でした！");
                    scanner.close();
                    return;
                default:
                    System.out.println("無効な入力です。1〜4の番号を入力してください。");
            }
        }
    }

    /**
     * レシピを新規に登録する処理を行う
     * ユーザーからタイトル、URL、使用する食材を入力させ、レシピリストに追加します。
     * * @param scanner          ユーザーからの入力を受け取るScanner
     * @param allRecipeList    追加先となるレシピリスト
     * @param ingredientMaster 登録可能な食材を取得するためのマスターデータ
     */
    private static void registerRecipe(Scanner scanner, AllRecipeList allRecipeList, IngredientMaster ingredientMaster) {
        System.out.print("レシピのタイトルを入力してください: ");
        String title = scanner.nextLine();
        
        System.out.print("レシピのURLを入力してください: ");
        String url = scanner.nextLine();

        ArrayList<Ingredient> selectedIngredients = new ArrayList<>();

        while (true) {
            System.out.println("\n--- カテゴリの選択 ---");
            IngredientCategory[] categories = IngredientCategory.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.println((i + 1) + ": " + categories[i].name());
            }
            System.out.println("0: 食材の追加を終了する");
            System.out.print("カテゴリの番号を入力してください: ");
            
            String catInput = scanner.nextLine();
            if (catInput.equals("0")) break;

            try {
                int catIndex = Integer.parseInt(catInput) - 1;
                if (catIndex >= 0 && catIndex < categories.length) {
                    IngredientCategory selectedCategory = categories[catIndex];
                    ArrayList<Ingredient> ingredientsInCat = ingredientMaster.searchIngredient(selectedCategory);

                    if (ingredientsInCat.isEmpty()) {
                        System.out.println("このカテゴリには食材が登録されていません。");
                        continue;
                    }

                    while (true) {
                        System.out.println("\n--- [" + selectedCategory.name() + "] の食材一覧 ---");
                        for (int i = 0; i < ingredientsInCat.size(); i++) {
                            System.out.println((i + 1) + ": " + ingredientsInCat.get(i).getName());
                        }
                        System.out.println("0: カテゴリ選択に戻る");
                        System.out.print("追加したい食材の番号を入力してください: ");
                        
                        String ingInput = scanner.nextLine();
                        if (ingInput.equals("0")) break;

                        try {
                            int ingIndex = Integer.parseInt(ingInput) - 1;
                            if (ingIndex >= 0 && ingIndex < ingredientsInCat.size()) {
                                Ingredient selected = ingredientsInCat.get(ingIndex);
                                if (!selectedIngredients.contains(selected)) {
                                    selectedIngredients.add(selected);
                                    System.out.println(">>> " + selected.getName() + " を追加しました！");
                                } else {
                                    System.out.println("その食材はすでに追加されています。");
                                }
                            } else {
                                System.out.println("正しい番号を入力してください。");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("数値を入力してください。");
                        }
                    }

                } else {
                    System.out.println("正しいカテゴリ番号を入力してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("数値を入力してください。");
            }
        }

        Recipe newRecipe = new Recipe(title, url, selectedIngredients);
        allRecipeList.addRecipe(newRecipe);
        System.out.println("\nレシピ「" + title + "」を登録しました！");
    }

    /**
     * 登録済みのレシピを削除する処理を行う
     * * @param scanner       ユーザーからの入力を受け取るScanner
     * @param allRecipeList 削除対象となるレシピリスト
     */
    private static void deleteRecipe(Scanner scanner, AllRecipeList allRecipeList) {
        ArrayList<Recipe> list = allRecipeList.getRecipeList();
        if (list.isEmpty()) {
            System.out.println("削除できるレシピがありません。");
            return;
        }

        System.out.println("\n--- レシピの削除 ---");
        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ": " + list.get(i).getTitle());
        }
        System.out.print("削除したいレシピの番号を入力してください（キャンセルは '0'）: ");
        
        String input = scanner.nextLine();
        if (input.equals("0")) return;

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < list.size()) {
                String removedTitle = list.get(index).getTitle();
                allRecipeList.deleteRecipe(index);
                System.out.println("レシピ「" + removedTitle + "」を削除しました。");
            } else {
                System.out.println("正しい番号を入力してください。");
            }
        } catch (NumberFormatException e) {
            System.out.println("無効な入力です。");
        }
    }

    /**
     * レシピを閲覧するためのサブメニューを表示し、検索方法の分岐を行う
     * * @param scanner          ユーザーからの入力を受け取るScanner
     * @param allRecipeList    検索対象のレシピリスト
     * @param ingredientMaster 食材検索時に使用するマスターデータ
     */
    private static void viewMenu(Scanner scanner, AllRecipeList allRecipeList, IngredientMaster ingredientMaster) {
        if (allRecipeList.getRecipeList().isEmpty()) {
            System.out.println("登録されているレシピはありません。");
            return;
        }

        while (true) {
            System.out.println("\n--- レシピの閲覧 ---");
            System.out.println("1. タイトル一覧から探す");
            System.out.println("2. 使う食材から探す");
            System.out.println("0. メインメニューに戻る");
            System.out.print("番号を入力してください: ");

            String input = scanner.nextLine();

            switch (input) {
                case "1":
                    viewRecipesByTitle(scanner, allRecipeList);
                    break;
                case "2":
                    searchRecipesByIngredient(scanner, allRecipeList, ingredientMaster);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("正しい番号を入力してください。");
            }
        }
    }

    /**
     * レシピをタイトル一覧から探し、詳細を表示する処理を行う
     * * @param scanner       ユーザーからの入力を受け取るScanner
     * @param allRecipeList 検索対象のレシピリスト
     */
    private static void viewRecipesByTitle(Scanner scanner, AllRecipeList allRecipeList) {
        ArrayList<Recipe> list = allRecipeList.getRecipeList();

        while (true) {
            System.out.println("\n--- タイトル一覧 ---");
            for (int i = 0; i < list.size(); i++) {
                System.out.println((i + 1) + ". " + list.get(i).getTitle());
            }
            System.out.println("0. 閲覧メニューに戻る");
            System.out.print("詳細を見たいレシピの番号を入力してください: ");

            String input = scanner.nextLine();
            if (input.equals("0")) break;

            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < list.size()) {
                    printRecipeDetail(list.get(index));
                    System.out.println("Enterキーを押すと一覧に戻ります...");
                    scanner.nextLine();
                } else {
                    System.out.println("正しい番号を入力してください。");
                }
            } catch (NumberFormatException e) {
                System.out.println("数値を入力してください。");
            }
        }
    }

    /**
     * ユーザーが指定した複数の食材を「すべて」使用しているレシピを検索（AND検索）して表示
     * * @param scanner          ユーザーからの入力を受け取るScanner
     * @param allRecipeList    検索対象のレシピリスト
     * @param ingredientMaster 食材を選択するために使用するマスターデータ
     */
    private static void searchRecipesByIngredient(Scanner scanner, AllRecipeList allRecipeList, IngredientMaster ingredientMaster) {
        ArrayList<Ingredient> targetIngredients = new ArrayList<>();

        while (true) {
            System.out.println("\n--- 検索する食材の追加 (現在 " + targetIngredients.size() + " 個選択中) ---");
            if (!targetIngredients.isEmpty()) {
                System.out.print("選択済み: ");
                for (Ingredient ti : targetIngredients) System.out.print("[" + ti.getName() + "] ");
                System.out.println();
            }

            System.out.println("1. 食材を追加する");
            if (!targetIngredients.isEmpty()) {
                System.out.println("2. この条件で検索を開始する");
            }
            System.out.println("0. キャンセルして戻る");
            System.out.print("番号を入力してください: ");

            String choice = scanner.nextLine();
            if (choice.equals("0")) return;

            if (choice.equals("2") && !targetIngredients.isEmpty()) {
                break;
            }

            if (choice.equals("1")) {
                System.out.println("\n--- カテゴリ選択 ---");
                IngredientCategory[] categories = IngredientCategory.values();
                for (int i = 0; i < categories.length; i++) {
                    System.out.println((i + 1) + ": " + categories[i].name());
                }
                System.out.print("カテゴリの番号を入力してください: ");
                
                try {
                    int catIndex = Integer.parseInt(scanner.nextLine()) - 1;
                    if (catIndex >= 0 && catIndex < categories.length) {
                        IngredientCategory selectedCategory = categories[catIndex];
                        ArrayList<Ingredient> ingredientsInCat = ingredientMaster.searchIngredient(selectedCategory);

                        if (ingredientsInCat.isEmpty()) {
                            System.out.println("このカテゴリには食材がありません。");
                            continue;
                        }

                        System.out.println("\n--- [" + selectedCategory.name() + "] の食材一覧 ---");
                        for (int i = 0; i < ingredientsInCat.size(); i++) {
                            System.out.println((i + 1) + ": " + ingredientsInCat.get(i).getName());
                        }
                        System.out.print("追加したい食材の番号を入力してください: ");
                        
                        int ingIndex = Integer.parseInt(scanner.nextLine()) - 1;
                        if (ingIndex >= 0 && ingIndex < ingredientsInCat.size()) {
                            Ingredient selected = ingredientsInCat.get(ingIndex);
                            if (!targetIngredients.contains(selected)) {
                                targetIngredients.add(selected);
                                System.out.println(">>> " + selected.getName() + " を検索条件に追加しました。");
                            } else {
                                System.out.println("その食材は既に選択されています。");
                            }
                        } else {
                            System.out.println("正しい番号を入力してください。");
                        }
                    } else {
                        System.out.println("正しいカテゴリ番号を入力してください。");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("数値を入力してください。");
                }
            }
        }

        ArrayList<Recipe> list = allRecipeList.getRecipeList();
        ArrayList<Recipe> matchedRecipes = new ArrayList<>();

        for (Recipe recipe : list) {
            if (recipe.getIngredients().containsAll(targetIngredients)) {
                matchedRecipes.add(recipe);
            }
        }

        System.out.println("\n=======================");
        System.out.print("【検索条件】: ");
        for (Ingredient ti : targetIngredients) System.out.print(ti.getName() + " ");
        System.out.println("\n-----------------------");

        if (matchedRecipes.isEmpty()) {
            System.out.println("該当するレシピは見つかりませんでした。");
        } else {
            System.out.println(matchedRecipes.size() + " 件のレシピが見つかりました。");
            for (Recipe recipe : matchedRecipes) {
                printRecipeDetail(recipe);
            }
        }
        System.out.println("=======================");
        System.out.println("Enterキーを押すとメニューに戻ります...");
        scanner.nextLine();
    }

    /**
     * 1つのレシピの詳細（タイトル、URL、必要食材）を画面に表示するための共通ヘルパーメソッド
     * * @param recipe 詳細を表示したいレシピオブジェクト
     */
    private static void printRecipeDetail(Recipe recipe) {
        System.out.println("\n■ " + recipe.getTitle());
        System.out.println("  URL      : " + recipe.getUrl());
        System.out.print("  必要食材 : ");
        
        ArrayList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            System.out.println("なし");
        } else {
            for (int i = 0; i < ingredients.size(); i++) {
                System.out.print(ingredients.get(i).getName());
                if (i < ingredients.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
    }
}