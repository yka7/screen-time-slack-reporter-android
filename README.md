# ScreenTime Slack Reporter

子ども端末の「1日の利用時間」と「使ったアプリ（アプリ別利用時間）」を、1日の終わりにSlackへ自動通知するAndroidアプリです。

## 機能

- 📊 **利用状況の取得**: UsageStatsManagerで当日0:00起点のアプリ別利用時間を取得
- 🔔 **Slack通知**: 1日1回、指定時刻にSlack Incoming WebhookへJSON投稿
- ⚙️ **除外アプリ設定**: 通知から除外したいアプリを設定可能
- 📱 **手動送信**: いつでも手動で送信可能
- 📝 **送信ログ**: 最終送信時刻とステータスを表示

## スクリーンショット

（TODO: アプリのスクリーンショットを追加）

## セットアップ

### 1. アプリのインストール

Android Studio でプロジェクトを開き、端末にインストールしてください。

```bash
./gradlew installDebug
```

### 2. 使用状況アクセス権限の付与

アプリを起動すると、使用状況へのアクセス権限を求められます。
設定画面で「ScreenTime Slack Reporter」を見つけて、アクセスを許可してください。

### 3. Slack Incoming Webhook の作成

1. アプリの設定画面で「Slack開発者ページを開く」をタップ
2. Slackアプリを作成（または既存のアプリを選択）
3. 「Incoming Webhooks」を有効化
4. 「Add New Webhook to Workspace」で投稿先チャンネルを選択
5. 生成されたWebhook URLをコピー

### 4. Webhook URLの設定

1. アプリの設定画面を開く
2. Webhook URLを貼り付け
3. 送信時刻を設定（デフォルト: 21:00）
4. 「自動送信を有効にする」をON
5. 「設定を保存」をタップ

### 5. テスト送信

「テスト送信」ボタンで正しく設定できているか確認できます。

## Slackへの通知例

```
📱 2026/01/04 (土) の利用状況
合計: 1時間42分 (目標30分との差: +72分)

• YouTube - 45分
• Chrome - 30分
• ゲームアプリ - 25分
• LINE - 12分
• その他 - 30分
```

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose
- **アーキテクチャ**: MVVM + Repository パターン
- **DI**: Hilt
- **非同期**: Coroutines + Flow
- **永続化**: Preferences DataStore
- **定時実行**: WorkManager
- **ネットワーク**: OkHttp

## プロジェクト構成

```
app/src/main/java/jp/co/screentime/slackreporter/
├── App.kt                          # Application
├── di/                             # Hilt DI モジュール
├── domain/
│   ├── model/                      # ドメインモデル
│   └── usecase/                    # ユースケース
├── data/
│   ├── repository/                 # リポジトリ
│   ├── settings/                   # DataStore
│   ├── usage/                      # UsageStats
│   └── slack/                      # Slack API
├── presentation/
│   ├── home/                       # ホーム画面 ViewModel
│   ├── settings/                   # 設定画面 ViewModel
│   ├── exclusions/                 # 対象外設定 ViewModel
│   └── model/                      # UI用モデル
├── ui/
│   ├── MainActivity.kt
│   ├── navigation/                 # Navigation
│   ├── screens/                    # Compose画面
│   └── theme/                      # テーマ
├── workers/                        # WorkManager
└── platform/                       # プラットフォーム固有
```

## 注意事項

### ⚠️ Webhook URLについて

Webhook URLは秘匿情報です。URLが漏洩すると、第三者がチャンネルに投稿できてしまいます。

- GitHubなどの公開リポジトリにコミットしない
- スクリーンショットにURLが映らないよう注意
- 不要になったらSlackのアプリ設定でWebhookを削除

### 📱 使用状況アクセス権限について

この権限は端末上の全てのアプリの利用状況を取得できる強力な権限です。
本アプリは以下の目的でのみ使用します：

- 当日のアプリ別利用時間の取得
- Slackへの通知

データは端末外への送信（Slack通知以外）は行いません。

## 開発者向け

### 環境構築

1. **Android Studioのインストール**
   - [Android Studio](https://developer.android.com/studio) をダウンロード・インストール
   - SDK Manager から Android SDK (API 35) をインストール

2. **プロジェクトのクローン**

   ```bash
   git clone https://github.com/your-repo/screen-time-slack-reporter.git
   cd screen-time-slack-reporter
   ```

3. **local.propertiesの設定**

   ```bash
   cp local.properties.example local.properties
   # sdk.dir を自分の環境に合わせて編集
   ```

4. **ビルド確認**

   ```bash
   ./gradlew assembleDebug
   ```

### テスト実行

```bash
# ユニットテスト
./gradlew testDebugUnitTest

# カバレッジレポート生成
./gradlew jacocoTestReport
# レポート: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### コードスタイル

- Kotlin公式スタイルガイドに準拠
- クリーンアーキテクチャ (domain/data/presentation/ui層)

## ライセンス

MIT License

## 作者

Jinno AI
