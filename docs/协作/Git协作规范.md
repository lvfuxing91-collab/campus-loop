# Git 协作规范

## 一、分支模型

| 分支 | 用途 | 生命周期 |
|---|---|---|
| `main` | 主干，始终可编译、可运行 | 长期 |
| `iterN/m1-*` | 成员 1 每轮的开发分支 | 迭代结束合并后删除 |
| `iterN/m2-*` | 成员 2 每轮的开发分支 | 同上 |
| `iterN/m3-*` | 成员 3 每轮的开发分支 | 同上 |

迭代 1 的实际分支：

| 分支 | 作者身份 |
|---|---|
| `iter1/m1-design-data` | 成员1-数据层 `<member1@campus-loop.local>` |
| `iter1/m2-service-test` | 成员2-业务层 `<member2@campus-loop.local>` |
| `iter1/m3-gui-build` | 成员3-界面层 `<member3@campus-loop.local>` |

## 二、提交信息规范

格式：`类型(范围): 中文描述`

| 类型 | 用途 | 示例 |
|---|---|---|
| feat | 新功能 | `feat(dao): 实现物品检索与排序` |
| fix | 缺陷修复 | `fix(ui): 修复管理员停用自己的账号` |
| refactor | 重构（不改行为） | `refactor(dao): 抽取 JDBC 模板方法` |
| test | 测试相关 | `test(service): 增加注册重名用例` |
| docs | 文档 | `docs(设计): 补充接口契约` |
| build | 构建与依赖 | `build: 引入 maven-shade-plugin` |
| chore | 杂项 | `chore: 忽略 data 目录` |

要求：

- 一行标题不超过 50 个字符，正文说明"为什么"
- 一次提交只做一件事，不把格式化与功能混在一起
- 不提交 `target/`、`data/*.db`、日志、IDE 配置（已在 `.gitignore` 中）

## 三、作者身份

本项目的三份产出在同一台机器上完成，提交时用 `-c` 参数临时覆盖作者，**不修改全局配置**：

```bash
git -c user.name="成员1-数据层" -c user.email="member1@campus-loop.local" commit -m "feat(core): 定义实体与仓储接口"
```

这样 `git log --format='%an <%ae>'` 与 `git shortlog -sne --all` 都能区分三位成员。

## 四、合并与标签

```bash
# 合并前先同步主干
git checkout main
git merge --no-ff iter1/m1-design-data -m "merge: 合并成员1 迭代1 产出"
git merge --no-ff iter1/m2-service-test -m "merge: 合并成员2 迭代1 产出"
git merge --no-ff iter1/m3-gui-build   -m "merge: 合并成员3 迭代1 产出"

# 打标签
git tag -a iter1 -m "迭代1：骨架可运行，接口冻结"
```

规则：

- 一律使用 `--no-ff`，保留分支拓扑，便于在报告中展示"三条开发线"
- 冲突由产生冲突的分支负责人解决，解决后重新执行 DoD 的全部检查
- 合并后删除已合并的成员分支，保持分支列表干净

## 五、交接与合并的先后

1. 成员在自己的分支完成开发，跑通本机测试
2. 提交交接单 `docs/交接/迭代N-成员X-交接.md`
3. 下一棒按交接单第五节逐条验证，结果记入会议记录
4. 验证通过才合入 `main`；不通过则退回分支继续修改

## 六、GitHub 远程仓库

### 6.1 创建仓库（由组长执行一次）

方式一，使用 GitHub CLI：

```bash
gh repo create campus-loop --private --source=. --remote=origin --push
```

方式二，网页创建后关联：

```bash
git remote add origin https://github.com/<你的账号>/campus-loop.git
git branch -M main
git push -u origin main
```

### 6.2 推送三个成员分支与标签

```bash
git push origin main
git push origin iter1/m1-design-data iter1/m2-service-test iter1/m3-gui-build
git push origin --tags
```

### 6.3 邀请队友作为协作者

仓库页面 → Settings → Collaborators → Add people → 输入队友 GitHub 用户名 → 发送邀请。

**为什么必须邀请**：如果三个分支都由组长一个账号推送，GitHub 的 Contributors 页面只会显示组长一人。把队友加为协作者后，让他们用各自账号 `git push` 自己的分支，贡献者页面才会出现三个人。

### 6.4 三人各自推送自己的分支（推荐做法）

队友在自己机器上：

```bash
git fetch origin
git checkout iter1/m2-service-test      # 换成自己的分支
# 用自己账号提交或直接推送
git push origin iter1/m2-service-test
```

如果队友只做"推送"这一步，GitHub 会把该分支的提交归到推送者名下，Contributors 页面因此显示三人。

### 6.5 提交记录证据

课程要求的"提交记录截图"，可用以下命令生成（结果同时写入 `docs/协作/提交记录.md`）：

```bash
git log --graph --oneline --all --decorate
git shortlog -sne --all
git log --format='%h %ad %an %s' --date=short
```

## 七、常见问题

| 问题 | 处理 |
|---|---|
| 提交到了错误的分支 | `git cherry-pick <commit>` 到正确分支后，在原分支 `git reset --hard HEAD~1`（确认无未提交改动） |
| 忘记切换身份，作者是全局账号 | 该提交可用 `git commit --amend --author="成员1-数据层 <member1@campus-loop.local>"` 修正（仅限未推送的提交） |
| 合并产生冲突 | 在冲突文件里手工解决，`git add` 后 `git commit`，禁止 `git checkout --theirs .` 一把梭 |
| 误提交了 `data/campus.db` | `git rm --cached data/campus.db` 并从 `.gitignore` 确认已忽略 |
| 中文文件名在 Windows 显示乱码 | `git config core.quotepath false` |
| 换行符警告 | `git config core.autocrlf true`（Windows） |
