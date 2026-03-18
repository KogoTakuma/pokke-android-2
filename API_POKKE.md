# NEW_POKKE API仕様書（最小通信版）

## 概要
熊野寮荷物管理アプリ「POKKE」のバックエンドAPI仕様。
タブレットアプリ（Android）とバックエンドサーバー間の**通信は最低限**に絞り、サーバーは主に「バックアップ」として荷物レコードを保持する。

- **ベースURL**: `https://api.pokke.kumano-ryo.com/v1`
- **認証方式**: API キー固定（`Authorization: Bearer <apiKey>` または `X-API-Key: <apiKey>`）
- **Content-Type**: `application/json`
- **文字エンコーディング**: UTF-8

### 通信の前提（重要）
- **サーバーが管理するもの**
  - 荷物レコード（Parcel）— バックアップ
  - 寮生名簿（Ryosei）— マスター
- **サーバーが管理しないもの（タブレット内のみ）**
  - イベント
  - 泊まり事務当番
  - 当番者情報
  - 操作ログ

### 必要な通信はこれだけ
| # | 通信 | 説明 |
|---|------|------|
| 1 | **/api/sync/pull** | サーバーの荷物・寮生情報でタブレットを更新 |
| 2 | **/api/sync/push** | タブレットの荷物情報をまとめてサーバーにバックアップ |
| 3 | **/api/call/notify** | 寮生の呼び出し（Discord DM） |

※ 寮生の登録・退寮・詳細取得は**不要**。サーバー側の名簿を取得してタブレットを上書きするだけ。

---

## 認証（API キー固定）

全APIリクエストで、以下のいずれかの形式で API キーを送信する。

```
Authorization: Bearer <apiKey>
```

または

```
X-API-Key: <apiKey>
```

- **API キーの取得**: サーバー管理者が事前に発行し、タブレットに設定する
- **タブレット側の保持**: `local.properties` や BuildConfig でビルド時に注入。リポジトリにはコミットしない
- **無効化**: 漏洩時はサーバー側でキーを無効化。タブレットには新しいキーを再設定する必要あり

※ ログイン処理は不要。リクエストごとに API キーを付与するだけ。

---

## 同期

### POST `/api/sync/pull`
サーバーから、タブレットのローカルDBを更新する。

- **parcels**: 差分取得（DIFF）または全件取得（SNAPSHOT）
- **ryosei**: 常に**サーバーの名簿で上書き**。SNAPSHOT のみ

**Request Body**:
```json
{
  "deviceId": "tablet-001",
  "parcels": {
    "mode": "DIFF",
    "since": 1710000000000
  },
  "ryosei": {
    "mode": "SNAPSHOT"
  }
}
```

`parcels.mode`: `DIFF` / `SNAPSHOT`

**Response** `200 OK`:
```json
{
  "serverTime": 1710086400000,
  "parcels": {
    "mode": "DIFF",
    "upserted": [
      {
        "id": "p-001",
        "createdAt": 1710000000000,
        "updatedAt": 1710086400000,
        "ryoseiId": "r-001",
        "ownerBlock": "A1",
        "ownerRoomName": "A101",
        "ownerName": "山田 太郎",
        "parcelType": "NORMAL",
        "note": null,
        "status": "REGISTERED",
        "isLost": false,
        "registeredByName": "佐藤 花子",
        "deliveredAt": null,
        "deliveredByName": null,
        "lastConfirmedAt": null
      }
    ]
  },
  "ryosei": {
    "mode": "SNAPSHOT",
    "version": 42,
    "items": [
      {
        "id": "r-001",
        "name": "山田 太郎",
        "nameKana": "やまだ たろう",
        "nameAlphabet": "Taro Yamada",
        "room": "A101",
        "block": "A1",
        "leavingDate": null,
        "discordStatus": "LINKED",
        "updatedAt": 1710086400000
      }
    ]
  }
}
```

補足:
- `ryosei.version` はタブレット側でのキャッシュ判定用
- 寮生はサーバーがマスター。登録・退寮はサーバー側で管理し、タブレットは取得して上書きするだけ

### POST `/api/sync/push`
Android（タブレット）から、**現在保持している荷物レコードをまとめて送信**してサーバーにバックアップする。バッチでOK。

**Request Body**:
```json
{
  "deviceId": "tablet-001",
  "generatedAt": 1710086400000,
  "parcels": {
    "mode": "SNAPSHOT",
    "items": [
      {
        "id": "p-001",
        "createdAt": 1710000000000,
        "updatedAt": 1710086400000,
        "ryoseiId": "r-001",
        "ownerBlock": "A1",
        "ownerRoomName": "A101",
        "ownerName": "山田 太郎",
        "parcelType": "NORMAL",
        "note": null,
        "status": "REGISTERED",
        "isLost": false,
        "registeredByName": "佐藤 花子",
        "deliveredAt": null,
        "deliveredByName": null,
        "lastConfirmedAt": null
      }
    ]
  }
}
```

**Response** `200 OK`:
```json
{
  "serverTime": 1710086400000,
  "accepted": {
    "parcels": 1
  }
}
```

補足:
- サーバー側では `id` をキーに upsert（同一IDは更新）
- 競合解決は `updatedAt` の新しい方を採用
- **Discord荷物到着通知**: サーバーは sync/push 受信後、新規登録荷物（status=REGISTERED）について寮生にDiscord DMを送信する。**複数荷物をまとめて処理**し、同一寮生宛は1通にまとめるなどしてパフォーマンス・レート制限に配慮する

---

## 呼び出し（寮生）

### POST `/api/call/notify`
寮生を呼び出す（来客・電話・書留など）。Discord DMで送信。

**Request Body**:
```json
{
  "ryoseiId": "r-001",
  "reason": "CALL",
  "message": "事務室まで来てください"
}
```

`reason`: `PARCEL_PICKUP` | `CALL` | `GENERAL`

**Response** `200 OK`:
```json
{
  "callId": "call-001",
  "ryoseiId": "r-001",
  "status": "ACCEPTED",
  "createdAt": 1710086400000
}
```

---

## エラーレスポンス

全APIで共通のエラー形式を使用する。

```json
{
  "error": "ERROR_CODE",
  "message": "人間が読めるエラーメッセージ",
  "details": {}
}
```

### HTTPステータスコード
| コード | 説明 |
|--------|------|
| `200` | 成功 |
| `400` | リクエスト不正（バリデーションエラー） |
| `401` | 認証エラー（API キー無効/未指定） |
| `403` | 権限不足 |
| `404` | リソース未発見 |
| `409` | 競合 |
| `422` | 処理不能（ビジネスロジックエラー） |
| `429` | レート制限超過 |
| `500` | サーバー内部エラー |

---

## データモデル定義（最小）

### RyoseiEntity
| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `id` | string | Yes | 寮生ID |
| `name` | string | Yes | 氏名（漢字） |
| `nameKana` | string | Yes | 氏名（ひらがな） |
| `nameAlphabet` | string | Yes | 氏名（ローマ字） |
| `room` | string | Yes | 部屋名 |
| `block` | string | Yes | ブロック |
| `leavingDate` | long? | No | 退寮日（ミリ秒、nullなら在寮中） |
| `discordStatus` | string | No | `LINKED` / `UNLINKED`（呼び出し可否判定用） |
| `updatedAt` | long | Yes | 更新日時（ミリ秒） |

### ParcelEntity
| フィールド | 型 | 必須 | 説明 |
|-----------|------|------|------|
| `id` | string | Yes | 荷物ID（ULID等） |
| `createdAt` | long | Yes | 登録日時（ミリ秒） |
| `updatedAt` | long | Yes | 更新日時（ミリ秒） |
| `ryoseiId` | string | Yes | 寮生ID |
| `ownerBlock` | string | Yes | 宛先ブロック |
| `ownerRoomName` | string | Yes | 宛先部屋 |
| `ownerName` | string | Yes | 宛先氏名 |
| `parcelType` | string | Yes | 荷物種別 |
| `note` | string? | No | 備考 |
| `status` | string | Yes | `REGISTERED` / `RECEIVED` |
| `isLost` | boolean | Yes | 紛失フラグ |
| `registeredByName` | string | Yes | 登録者名（事務当など） |
| `deliveredAt` | long? | No | 引渡日時 |
| `deliveredByName` | string? | No | 引渡者名 |
| `lastConfirmedAt` | long? | No | 泊まり事務当番の最終確認日時（ミリ秒） |

`parcelType` の値:
| 値 | 説明 |
|----|------|
| `NORMAL` | 普通 |
| `REFRIGERATED` | 冷蔵 |
| `FROZEN` | 冷凍 |
| `LARGE` | 大型 |
| `ABSENCE_SLIP` | 不在票 |
| `OTHER` | その他 |
