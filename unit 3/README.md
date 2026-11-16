# Programming II — Semester 1 (Data Structures & Algorithms)

This repository is your coding environment for the semester: Python and Clojure, preinstalled and ready to go in a GitHub Codespace. You don't install anything on your own computer — everything runs in the cloud, in your browser (or the desktop/mobile Codespaces app if you prefer).

## What's already set up for you

- Python 3.12
- Java 21 (needed to run Clojure)
- Clojure CLI tools + Leiningen
- The Calva extension (Clojure support for VS Code), preinstalled

You don't need to install any of this yourself — it's all baked into the Codespace automatically the first time it starts.

---

## Step 1: Fork this repository

Forking makes your own personal copy of this repo under your own GitHub account. You need this because you can't save changes directly to the class's original template — only to your own copy.

1. Go to this repository's page on GitHub.
2. Click the **Fork** button in the top-right corner.
3. Confirm the fork under your own GitHub account.
4. You'll be redirected to **your own copy** of the repo — the URL will now show your username instead of the class's, like `github.com/your-username/semester1-template` instead of the original.

**Do all of your work in your fork, not the original repository.** Bookmark your fork's URL now so you can find it again quickly.

---

## Step 2: Open your fork in a Codespace

1. On **your fork's** page (double-check the URL shows your username), click the green **Code** button.
2. Click the **Codespaces** tab.
3. Click **Create codespace on main**.

The first time you do this, GitHub has to build the environment from scratch — installing Python, Java, the Clojure CLI, and Calva. This can take a couple of minutes. You'll see a terminal window open automatically and run a setup script (`setup.sh`) — let it finish completely before doing anything else. When it's done, you'll see a message like `Setup complete!` in the terminal.

**After the first time**, reopening the same Codespace is much faster, since nothing needs to be reinstalled.

---

## Step 3: Verify everything works

Open a terminal in the Codespace (if one isn't already open: **Terminal → New Terminal**) and try:

```bash
python3 --version
```
```bash
clojure -e "(println \"Clojure is working!\")"
```

If both print something sensible (a Python version number, and "Clojure is working!"), you're ready to go.

---

## Step 4: Do your work

Create and edit files right inside the Codespace, the same way you would in any code editor. Run Python files with:
```bash
python3 your_file.py
```
Run Clojure files or start a REPL through Calva (Command Palette → "Calva: Start a Project REPL and Connect").

**Important:** anything you create or change only exists *inside this Codespace* until you save it back to GitHub. If the Codespace is ever deleted, unsaved work goes with it. That's what the next step is for.

---

## Step 5: Save your work — commit and push

This is the single most important habit to build all semester. GitHub does not automatically save your work anywhere permanent — you have to explicitly tell it to, using **git**. There are two ways to do this inside a Codespace; use whichever feels more comfortable.

### Option A: The terminal
```bash
git add .
git commit -m "Describe what you did, e.g. 'finish Day 21 stack practice'"
git push
```
- `git add .` stages every changed file to be saved.
- `git commit -m "..."` saves a snapshot with a short description — the message should say what you actually did, not just "updates."
- `git push` sends that snapshot up to **your fork** on GitHub, where it's safe.

### Option B: The Source Control panel (VS Code GUI)
1. Click the **Source Control** icon in the left sidebar (it looks like a branching line, and shows a number badge when you have unsaved changes).
2. Type a short commit message in the box at the top.
3. Click the checkmark (**Commit**).
4. Click **Sync Changes** (or the push icon) to send it to GitHub.

### Why this matters
Work that exists only in a Codespace is not safe — Codespaces can be stopped, can time out, and can eventually be deleted if left unused. **The only work that's guaranteed safe is work that's been pushed to your fork on GitHub.** Get in the habit of committing and pushing at natural stopping points — the end of an exercise, the end of class, anytime you'd be upset to lose what you just did — not just once at the very end of the semester.

---

## Managing your Codespace (hours & storage)

GitHub's free tier gives every personal account **120 core-hours of compute per month** (which works out to about 60 hours of active use on the default 2-core machine) and **15 GB of storage**. A few things worth knowing:

- **Codespaces auto-stop** after about 30 minutes of inactivity by default — this is intentional and saves your compute hours, not a bug. Reopening a stopped Codespace is quick.
- **Stopped Codespaces still use storage quota**, even while not running. If you're not using an old Codespace anymore, delete it (go to [github.com/codespaces](https://github.com/codespaces), find it in the list, and delete it) to free up space.
- You generally don't need more than one Codespace per repo — reopen the same one each class rather than creating a new one every time.

---

## Troubleshooting

**The Codespace seems stuck or broken.**
Try rebuilding it: open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and run **"Codespaces: Rebuild Container."** This re-runs the setup from scratch without losing your files (as long as they're saved — see Step 5).

**I can't tell if I'm in my fork or the original repo.**
Check the URL at the top of the browser — it should show your GitHub username, not the class's. If it doesn't, you're in the wrong place.

**I pushed my code but don't see it on GitHub.**
Refresh the page, and double-check you're looking at your fork (not the original) and the right branch (usually `main`). If `git push` printed an error in the terminal, read it — it usually explains exactly what went wrong.

**If you're ever stuck, ask your teacher** — don't spend more than a few minutes stuck on an environment issue before asking.
