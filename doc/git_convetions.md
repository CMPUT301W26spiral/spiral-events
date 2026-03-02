# Git Conventions
---

## 1. Branch Naming

Use a consistent prefix system for all branches. **Never commit directly to `main`** — always go through `dev` first.

| Branch | Purpose |
|--------|---------|
| `main` | Stable, working code only |
| `dev` | Integration branch — merge features here first |
| `feature/add-city-screen` | New feature branches |
| `bugfix/fix-crash-on-back-button` | Bug fix branches |
| `hotfix/fix-critical-login-bug` | Urgent production fixes |
| `test/add-espresso-tests` | Test-only branches |

---

## 2. Commit Messages

Follow this structure for all commits. Keep the first line **under 72 characters** and written in **present tense** ("add" not "added").

| Type | Example |
|------|---------|
| `feat` | `feat: add ShowActivity for city details` |
| `fix` | `fix: resolve null pointer on back button click` |
| `test` | `test: add espresso tests for ShowActivity` |
| `refactor` | `refactor: clean up MainActivity onClick logic` |
| `docs` | `docs: update README with setup instructions` |
| `chore` | `chore: update gradle dependencies` |

> **Tip:** Link commits to GitHub Issues using `#issue_number`:
> ```
> feat: add back button to ShowActivity (#12)
> ```

---

## 3. Pull Request (PR) Conventions

- Every feature needs a **PR to merge into `dev`** — no direct pushes
- At least **1–2 teammates must review and approve** before merging
- PR title should match the branch name (e.g. `feature/add-city-screen`)
- **Delete the branch** after merging to keep the repo clean

---

## 4. General Workflow

Follow this flow for every feature or fix:

```
1. Pull latest dev branch
2. Create your feature branch off dev
3. Make small, frequent commits as you work
4. Push your branch and open a PR to dev
5. Get it reviewed and approved by 1-2 teammates
6. Merge into dev
7. Periodically merge dev into main when stable
```

---

## 5. Android-Specific Tips

### .gitignore
Make sure your `.gitignore` excludes these generated files — they cause constant merge conflicts if committed:

```
/.gradle
/local.properties
/.idea
/build
*.iml
```

---

## 6. Avoiding Merge Conflicts

With 7 developers, conflicts are likely. Follow these rules to minimize them:

- **One person owns one file** where possible — avoid multiple people editing `MainActivity.java` simultaneously
- **Split work by feature** — each person works on their own Activity or fragment
- **Commit and push frequently** (small commits) rather than sitting on large changes
- **Pull from `dev` into your branch regularly** to stay up to date

---

## 7. Recommended Tools

- **GitHub Projects or Issues** — assign tasks so two people don't work on the same thing
- **Link commits to issues** using `#issue_number` in commit messages to track progress automatically
- **Require PR reviews** in GitHub branch protection settings to enforce code review
