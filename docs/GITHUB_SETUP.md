# GitHub setup

This file is for the first push only. It is intentionally simple.

1. Extract the project ZIP.
2. Open PowerShell or Git Bash inside the extracted `jvm-spark-performance-lab` folder.
3. Create an empty GitHub repository with the same or a similar name. Do not initialize it with another README or `.gitignore`.
4. Run:

```bash
git init
git branch -M main
git add .
git status
git commit -m "Initial JVM and Spark performance lab"
git remote add origin https://github.com/<YOUR_USERNAME>/jvm-spark-performance-lab.git
git remote -v
git push -u origin main
```

Use your own GitHub username and repository URL. Authenticate using GitHub's supported credentials flow when Git prompts you.
