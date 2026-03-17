# POKKE App - 熊野寮荷物管理アプリ

## 概要
熊野寮事務室での荷物受け取り・引き渡し・管理を行うタブレットアプリ。
旧アプリ (kumano-dormitory/pokke-app-android) を最新技術で再構築。

## 技術スタック
- **言語**: Kotlin 2.1
- **UI**: Jetpack Compose + Material 3
- **DB**: Room (SQLite) — タブレット単体で完結
- **ナビゲーション**: Navigation Compose
- **非同期**: Kotlin Coroutines + Flow
- **ビルド**: Gradle 8.11 + AGP 8.7 + Version Catalog (libs.versions.toml)
- **SDK**: compileSdk 35 / targetSdk 35 / minSdk 28

## プロジェクト構成
```
pokke-app/
├── app/src/main/java/com/kumanodormitory/pokke/
│   ├── MainActivity.kt          # エントリポイント
│   ├── ui/
│   │   ├── PokkeApp.kt          # ルートComposable + Navigation
│   │   ├── theme/Theme.kt       # Material 3 テーマ
│   │   └── screen/              # 各画面のComposable
│   ├── data/
│   │   ├── local/               # Room DB, Entity, DAO
│   │   ├── model/               # ドメインモデル（未実装）
│   │   └── repository/          # リポジトリ層
│   └── di/                      # DI（未実装）
├── gradle/libs.versions.toml    # 依存関係の一元管理
└── CLAUDE.md                    # この文書
```

## 開発ルール
- 画面はすべて Jetpack Compose で実装する（XMLレイアウト不使用）
- 状態管理は ViewModel + StateFlow パターン
- DB操作は必ず Repository 経由
- テストは `app/src/test/` (Unit) と `app/src/androidTest/` (UI)

## 旧アプリからの主要機能（移行対象は別途決定）
- 事務当番交代 (JimutoChange)
- 荷物受け取り登録 (Register) — 普通/冷蔵/冷凍/大型/不在票/その他
- 荷物引き渡し (Release) + 代理人対応
- 泊まり事務当番確認 (NightDuty)
- QRコード読み取り (ZXing)
- サーバー同期（将来的にバックエンド再構築後）

## バックエンド
- 現時点ではタブレット単体で完結（ローカルDB only）
- バックエンドは別プロセスで再構築予定
- 将来的にAPI連携を追加する想定（Repository層で吸収）
