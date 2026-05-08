# RecipeManager

## 概要
- ネット上(クックパッドやYoutubeなど)のレシピを料理するたびに検索するのめんどくさい！
- Youtubeショートでやってみたいレシピあったけど忘れそう！
- 家に残ってる野菜でなんか作れないかな？ 

そんなあなたに！タイトルとレシピのURLと食材のみを記録するだけのこのアプリ！

## 背景
- Youtube垢に作ったお料理リストに登録した動画が100を超えてしまい、管理が面倒
- 家にある食材だけで作りたいのに、レシピを調べると何かしらの食材や調味料が不足してる

といったことがあり、作成。あと登録作業面倒だからAIに丸投げできるようにした。

## 前提条件
- Windows または MacOS またはLinux OS
- JDK17以降
- Ollama
- Ollamaの対応済みプロバイダ
  - gemma4:e2b
  - gemma4:e4b
  - llama3:8b
  - mistral
  - mistral:7b
  - mistral-nemo
  - mistral-small
  - mixtral
  - phi4
  - phi3.5
  - phi3
  - phi3:mini
  - phi3:medium
  - qwen3.5:4b
  - qwen3.5:9b
  - qwen3-vl:4b
  - qwen3-vl:8b

Ollamaが無くても動作しますが、AI機能はAPIキーを用いるものしか動作しなくなります。

## 使い方
### 実行方法
起動スクリプトは `scripts/` に OS 別のディレクトリで分けて格納しています。
ダブルクリック起動が可能です(初回起動時はビルドが走るため少し時間がかかります)。

```
scripts/
├── windows/    Windows 用 (.bat)
│   ├── run.bat
│   ├── run-debug.bat
│   ├── build.bat
│   └── setup-whisper.bat
└── unix/       macOS / Linux 用 (.command)
    ├── run.command
    ├── run-debug.command
    ├── build.command
    └── setup-whisper.command
```

#### Windows
`scripts/windows/run.bat` をダブルクリックするだけで起動できます。初回はWhisperなどのツールを導入するかを選択してください。
![RMP_Run.png](Photo/RMP_Run.png)\
初回起動後にWhisperを導入する場合は `scripts/windows/setup-whisper.bat` をダブルクリックしてください。

| ファイル                                | 用途                                              |
|-------------------------------------|-------------------------------------------------|
| **`scripts/windows/run.bat`**           | アプリを起動（コンソール非表示）初回はWhisper、ytdlp、ffmpegを導入するか選択 |
| **`scripts/windows/run-debug.bat`**     | コンソール付きで起動。エラー調査用                               |
| **`scripts/windows/build.bat`**         | コンパイルして RecipeManager.jar を生成                   |
| **`scripts/windows/setup-whisper.bat`** | Whisper、ytdlp、ffmpegを導入                         |

#### macOS
Finder で `scripts/unix/run.command` をダブルクリックすると Terminal で起動します。
**初回のみ** OS の Gatekeeper で「開発元を確認できないため開けません」と出る場合があります。その場合は
ファイルを **右クリック → 開く** で警告を回避してください(2回目以降は普通にダブルクリックできます)。
端末から実行する場合は:
```bash
./scripts/unix/run.command
```

#### Linux
ファイルマネージャの設定によってはダブルクリックでテキストエディタが開いてしまうことがあります。
その場合は端末から実行してください:
```bash
./scripts/unix/run.command
```
GNOME / KDE 等では「実行可能なファイルとして開く」設定にすればダブルクリック起動も可能です。

| ファイル                                       | 用途                                                       |
|--------------------------------------------|----------------------------------------------------------|
| **`scripts/unix/run.command`**             | アプリをバックグラウンド起動。初回はビルドも実行                                 |
| **`scripts/unix/run-debug.command`**       | ターミナル付きで起動。stdout/stderrが見える(エラー調査用)                     |
| **`scripts/unix/build.command`**           | コンパイルして RecipeManager.jar を生成                            |
| **`scripts/unix/setup-whisper.command`**   | Whisper, yt-dlp, ffmpeg を導入(macOS は brew、Linux は手順表示) |

> ファイルが実行可能でない場合は `chmod +x scripts/unix/*.command` で権限を付与してください。

#### Maven 対応IDE 
pom.xml の依存を解決して `main.java.SwingMain` を Run。`lib/` の jar はリポジトリから自動取得されます。

##### ビルド済みJARから起動する場合
`scripts/windows/build.bat` または `scripts/unix/build.command` で生成した `RecipeManager.jar` には
`Main-Class` と `Class-Path` が記載されているので、クラスパス指定なしで実行できます:
```
java -jar RecipeManager.jar
```

---
## 使い方

### 起動後
![RMP_default.png](Photo/RMP_default.png)

サイドメニューの「☀ ライトモード / ☾ ダークモード」ボタンでテーマを切り替えられます。選択は次回起動時にも引き継がれます (Java Preferences API に保存)。

### レシピ登録
![RMP_Regist.png](Photo/RMP_Regist.png)

タイトル、URL、食材を入力し、レシピ保存ボタンを押すと初回起動時に作成される `recipes.db` (SQLite) に保存されます。\
食材は既存のものから選択する方式であり、食材の追加をしたい場合は`database.csv`を編集してください。

| カテゴリー名       | 想定カテゴリー |
|--------------|---------|
| VEGETABLE    | 野菜      |
| MEAT         | 肉系      |
| SEAFOOD      | 海鮮系     |
| CARBOHYDRATE | 炭水化物    |
| FRUIT        | 果物      |
| MILK         | 乳製品     |
| PICKLES      | 漬物、発酵食品 |
| SEASONING    | 調味料     |
| OTHER        | その他     |

AIを登録している場合はURLを入力すると、自動的にタイトルと食材を入力します。(AIの設定については下のページ参照)
また、登録されていない食材が来た場合は`OTHER`カテゴリーとして登録されます。

### レシピ閲覧

登録したレシピの閲覧ができます。 レシピは検索方法が3つ あります。\
表示されているURLはクリックするとブラウザーが起動します。\
また、"右下の選択中レシピを編集"から選択しているレシピを編集することができます。

#### タイトルから検索
![RMP_View_Title.png](Photo/RMP_View_Title.png)

#### 食材から検索
![RMP_View_Ingredient.png](Photo/RMP_View_Ingredient.png)

#### カテゴリーから検索
![RMP_View_Category.png](Photo/RMP_View_Category.png)

### レシピ削除
![RMP_Delete.png](Photo/RMP_Delete.png)

### AI設定
![RMP_AI.png](Photo/RMP_AI.png)

レシピ登録で利用するAIの設定を行います。
AIは4種類から選べます。
- ChatGPT
- Gemini
- Claude
- Ollama

ChatGPTとGeminiとClaudeはAPIキーを用いてオンライン上で、Ollamaはローカル環境で処理するものとなります。\
AI設定は保存されます。APIキーは暗号化した状態で保存します。

### データ入出力
![RMP_DataI-O.png](Photo/RMP_DataI-O.png)

レシピデータをJSONファイル化し、データ共有することができます。\
インポートしたレシピ内の食材が存在しない場合は、database.csvにOTHERカテゴリーとして登録されます。

#### URLバッチ取込み (AI)
1行に1つの URL を記述したテキスト(.txt)ファイルを選択すると、各 URL を**現在選択中の AI プロバイダー**で順番に解析して一括登録できます。\
クックパッドのお気に入り一覧やYoutubeの再生リストから抽出した URL をまとめて取り込むときに便利です。

- 空行と `#` で始まるコメント行は無視されます
- カテゴリーは「その他」固定で登録されるため、必要に応じてレシピ閲覧画面の「編集」から付け直してください
- 1件あたり10〜60秒程度かかります(モデル/動画の長さに依存)。途中でキャンセル可能(現在処理中のリクエスト完了後に停止)

入力ファイル例 (`urls.txt`):
```
# 残り野菜消化レシピ集
https://cookpad.com/recipe/0000001
https://www.youtube.com/watch?v=xxxxxxxxxxx
https://youtu.be/yyyyyyyyyyy
```

---
## 補足
### AIについて
APIを使うものに関しては利用可能なトークン数に注意してください。~~石油王は気にしないでおk~~\
Ollama利用するときは事前にOllamaを起動し、使いたいモデルをDLしてください。
また、モデルによってはデバイスのリソースを大量に取ってしまうため注意。~~石油王h(略)~~\
目安として
- メモリー8GB程度 → 2Bモデル
- メモリー16GB程度 → 4Bモデル
- メモリー16GB程度+VRAM8GB程度 → 8Bモデル
- メモリー32GB程度+VRAM16GB → 12Bモデル

ちなみに自分のM2MacBook(8GB)は2Bモデルで撃沈しました。

### Youtube動画を参照するときについて
Youtube動画はタイトルと字幕と概要欄を利用します。\

- GeminiはYoutubeを分析する機能があるため、Whisperを導入する必要がないです。**なんなら容量の無駄遣いになります。**
- Gemini以外は概要欄に材料が記載されていれば概要欄を利用し、なければ字幕を利用、字幕もなければWhisperで字幕を生成し、それを利用します。\
なおWhisperを利用した場合の動作は不安定のため、 **Geminiを推奨** します。

#### Whisper のセットアップ
| OS      | 方法                                                                                                                                            |
|---------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| Windows | `scripts/windows/setup-whisper.bat` をダブルクリック (yt-dlp / ffmpeg / whisper.cpp / モデルを `lib/tools/`, `lib/models/` に配置)                              |
| macOS   | `scripts/unix/setup-whisper.command` を実行 (Homebrew で `yt-dlp` / `ffmpeg` / `whisper-cpp` を導入し、モデルだけ `lib/models/` に取得)                            |
| Linux   | `scripts/unix/setup-whisper.command` がパッケージ管理コマンドの案内を表示。`yt-dlp` / `ffmpeg` / `whisper-cli` を PATH に通したうえで、モデル `lib/models/ggml-base.bin` だけ取得すれば動作します。 |

UNIX 環境では `lib/tools/` にバイナリがなくても、PATH 上の `yt-dlp` / `ffmpeg` / `whisper-cli`(または `whisper-cpp`)を自動検出します。
---
## ライセンス
### 同梱しているサードパーティライブラリ(`lib`ディレクトリ内)

| ライブラリ | バージョン | ライセンス |
|---|---|---|
| [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) | 3.45.3.0 | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [SLF4J API](https://www.slf4j.org/) | 2.0.13 | [MIT License](https://www.slf4j.org/license.html) |
| [SLF4J NOP](https://www.slf4j.org/) | 2.0.13 | [MIT License](https://www.slf4j.org/license.html) |
| [FlatLaf](https://www.formdev.com/flatlaf/) | 3.4.1 | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

### `scripts/windows/setup-whisper.bat` で取得する外部ツール

| ツール | ライセンス | 備考                                      |
|---|---|-----------------------------------------|
| [yt-dlp](https://github.com/yt-dlp/yt-dlp) | [Unlicense](https://github.com/yt-dlp/yt-dlp/blob/master/LICENSE) | パブリックドメイン                               |
| [ffmpeg](https://ffmpeg.org/) (BtbN gpl ビルド) | **[GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html)** | `ffmpeg-master-latest-win64-gpl.zip` 利用 |
| [whisper.cpp](https://github.com/ggml-org/whisper.cpp) | [MIT License](https://github.com/ggml-org/whisper.cpp/blob/master/LICENSE) |                                         |
| [ggml Whisper モデル](https://huggingface.co/ggerganov/whisper.cpp) | [MIT License](https://github.com/openai/whisper/blob/main/LICENSE) | OpenAI Whisper                          |

---

#### 今後の予定

- AndroidとかiOSとかで動かしたい
