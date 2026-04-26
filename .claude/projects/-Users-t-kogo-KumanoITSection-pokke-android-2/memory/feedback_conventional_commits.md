---
name: conventional commits per semantic unit
description: User requires conventional commits grouped by semantic/logical unit of change
type: feedback
---

コミットは conventional commit 形式で、意味単位ごとに分けて作成すること。

**Why:** ユーザーが明示的にルールとして徹底するよう指示した。
**How to apply:** 変更をコミットする際、機能追加・バグ修正・リファクタリングなどの意味単位ごとに分割し、`feat:`, `fix:`, `refactor:`, `style:`, `chore:` 等のプレフィックスを付ける。一度に全変更をまとめず、論理的に独立した変更ごとにコミットする。
