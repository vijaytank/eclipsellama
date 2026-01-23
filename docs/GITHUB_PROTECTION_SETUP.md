# GitHub Repository Protection Setup

## Branch Protection Rules

To protect your `main` and `develop` branches, follow these steps:

### 1. Go to Repository Settings
1. Navigate to: https://github.com/vijaytank/eclipsellama/settings
2. Click **Branches** in the left sidebar

### 2. Protect `main` Branch

Click **Add branch protection rule** and configure:

**Branch name pattern:** `main`

**Protection settings:**
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: 1
  - ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ **Require status checks to pass before merging**
  - (Add CI checks if you set them up later)
- ✅ **Require conversation resolution before merging**
- ✅ **Do not allow bypassing the above settings** (even for admins)
- ✅ **Restrict who can push to matching branches**
  - Only you (vijaytank)

Click **Create** or **Save changes**

### 3. Protect `develop` Branch

Repeat the same process for `develop`:

**Branch name pattern:** `develop`

**Protection settings:**
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: 1
- ✅ **Require conversation resolution before merging**
- ⚠️ **Allow force pushes** (optional - for rebasing)

### 4. Set Default Branch

1. In **Settings → Branches**
2. Set **Default branch** to `develop`
3. This ensures PRs target `develop` by default

---

## Additional Security Settings

### Enable Dependabot
1. Go to **Settings → Security → Code security and analysis**
2. Enable:
   - ✅ Dependency graph
   - ✅ Dependabot alerts
   - ✅ Dependabot security updates

### Require Signed Commits (Optional)
1. In branch protection rules
2. ✅ **Require signed commits**
3. Contributors must sign commits with GPG

---

## Workflow After Protection

### For You (Maintainer)
1. Create feature branch: `git checkout -b feature/new-feature`
2. Make changes and commit
3. Push: `git push origin feature/new-feature`
4. Create PR on GitHub
5. Review and merge (even your own PRs need approval if you enable it)

### For Contributors
1. Fork the repository
2. Create feature branch
3. Make changes
4. Create PR to `develop` branch
5. Wait for review and approval

---

## Files Added
- ✅ `CONTRIBUTING.md` - Contribution guidelines
- ✅ `SECURITY.md` - Security policy

**Commit and push these files:**
```bash
git add CONTRIBUTING.md SECURITY.md
git commit -m "docs: add contributing guidelines and security policy"
git push origin develop
```

Then set up branch protection on GitHub!
