# Semantic Commit Cheat Sheet

> Based on [Conventional Commits v1.0.0](https://www.conventionalcommits.org/) — the standard used by `semantic-release` to automate versioning and changelogs.

---

## Commit Structure

```
<type>(<scope>): <short description>

[optional body]

[optional footer(s)]
```

| Part | Required | Notes |
|---|---|---|
| `type` | ✅ | Determines version bump |
| `scope` | ❌ | What area of the codebase changed |
| `description` | ✅ | Imperative, lowercase, no period |
| `body` | ❌ | More detail, wrap at 72 chars |
| `footer` | ❌ | Breaking changes, issue refs |

---

## Types & Version Bumps

| Type | Version Bump | When to use |
|---|---|---|
| `feat` | `MINOR` (**1.x.0**) | New feature for the user |
| `fix` | `PATCH` (**1.0.x**) | Bug fix for the user |
| `perf` | `PATCH` (**1.0.x**) | Performance improvement |
| `revert` | `PATCH` (**1.0.x**) | Reverts a previous commit |
| `docs` | none | Documentation only |
| `style` | none | Formatting, whitespace, semicolons |
| `refactor` | none | Code restructure, no behavior change |
| `test` | none | Adding or fixing tests |
| `build` | none | Build system, external dependencies |
| `ci` | none | CI/CD config changes |
| `chore` | none | Maintenance, no production code change |
| `BREAKING CHANGE` | `MAJOR` (**x.0.0**) | Any breaking API change — see below |

---

## Examples by Type

### `feat` — New feature → MINOR bump
```
feat(auth): add JWT refresh endpoint
feat(search): support fuzzy matching on username
feat: add dark mode toggle
```
> Use when you ship something **new** that users/consumers of the API can use.

---

### `fix` — Bug fix → PATCH bump
```
fix(db): handle null user_id in query builder
fix(auth): prevent token reuse after logout
fix: correct off-by-one in pagination
```
> Use when you **correct broken behavior**. Not for new features.

---

### `perf` — Performance → PATCH bump
```
perf(cache): replace Redis KEYS with SCAN
perf(images): lazy-load thumbnails on scroll
```
> Use for measurable performance improvements with **no behavior change**.

---

### `revert` — Undo a commit → PATCH bump
```
revert: feat(auth): add JWT refresh endpoint

Reverts commit a3f92bc.
```
> Include the original commit hash in the body when possible.

---

### `docs` — Documentation → no bump
```
docs: update deployment instructions
docs(api): document /health endpoint response shape
docs(readme): add local dev setup steps
```
> README, JSDoc, OpenAPI specs, changelogs. **No code changes.**

---

### `style` — Formatting → no bump
```
style: enforce single quotes via prettier
style(auth): reorder imports alphabetically
```
> Whitespace, formatting, semicolons. **Zero logic change.**  
> Not to be confused with CSS/UI styling — use `feat` or `fix` for that.

---

### `refactor` — Code restructure → no bump
```
refactor(user): extract validation into UserValidator class
refactor: replace callbacks with async/await
```
> Neither fixes a bug nor adds a feature. Improves **internal structure only.**

---

### `test` — Tests → no bump
```
test(auth): add unit tests for token expiry edge case
test: increase coverage for UserService
```
> Adding, fixing, or restructuring tests. No production code changes.

---

### `build` — Build system → no bump
```
build: upgrade Gradle wrapper to 8.5
build(docker): add multi-stage build for smaller image
build: configure webpack bundle analyzer
```
> Changes to build tools, compilers, package managers.

---

### `ci` — CI/CD config → no bump
```
ci: add semantic-release to main.yml
ci(deploy): cache Gradle dependencies between runs
ci: fail fast on test step
```
> GitHub Actions, Jenkins, CircleCI configs. **Not** application code.

---

### `chore` — Maintenance → no bump
```
chore: bump dependencies
chore: remove unused env variables
chore(deps): update spring-boot to 3.2.1
```
> Catch-all for housekeeping that doesn't fit elsewhere.  
> Dependency bumps that don't change behavior go here.

---

## Breaking Changes → MAJOR bump

Two ways to signal a breaking change:

### 1. `!` after the type/scope (short form)
```
feat(api)!: remove v1 endpoints
fix(auth)!: tokens are now short-lived by default
```

### 2. `BREAKING CHANGE:` footer (long form)
```
feat(api): migrate to v2 response envelope

BREAKING CHANGE: all responses are now wrapped in { data, meta }.
Clients consuming the raw response must be updated.
```

### Both together (most explicit)
```
feat(api)!: migrate to v2 response envelope

Replaces the flat response structure with a wrapped envelope.

BREAKING CHANGE: all responses are now wrapped in { data, meta }.
Clients consuming the raw response must be updated.
Refs: #142
```

> ⚠️ `BREAKING CHANGE` in the footer **must** be all caps and followed by a colon.

---

## Scopes

Scopes are optional but strongly recommended on larger projects. Keep them short and consistent across the team.

```
feat(auth): ...       # authentication / authorization
feat(api): ...        # REST API layer
feat(db): ...         # database / queries / migrations
feat(ui): ...         # frontend components
feat(search): ...     # search functionality
feat(payments): ...   # payment processing
feat(infra): ...      # infrastructure / DevOps
feat(config): ...     # app configuration
```

> Define your scopes in `CONTRIBUTING.md` so the whole team uses the same ones.

---

## Linking Issues & PRs

Reference issues in the footer — semantic-release can close them automatically:

```
fix(db): prevent duplicate email inserts

Adds a unique constraint migration and catches the resulting
IntegrityError at the service layer.

Closes #88
Refs #91, #93
```

| Footer keyword | Effect |
|---|---|
| `Closes #N` | Closes the issue on merge |
| `Fixes #N` | Same as Closes |
| `Refs #N` | Links without closing |
| `Co-authored-by: Name <email>` | Credits co-authors |

---

## Semantic Release Version Logic

```
feat(auth)!: ...   →   BREAKING CHANGE  →  2.0.0  (MAJOR)
feat(auth): ...    →   new feature      →  1.1.0  (MINOR)
fix(auth): ...     →   bug fix          →  1.0.1  (PATCH)
docs: ...          →   no release       →  —
chore: ...         →   no release       →  —
```

> Version is determined by the **highest-impact** commit since the last tag.  
> One `feat` + ten `fix` commits → **MINOR** bump only.

---

## Quick Rules

- ✅ Use **imperative mood**: `add`, `fix`, `remove` — not `added`, `fixes`, `removed`
- ✅ **Lowercase** type, scope, and description
- ✅ **No period** at the end of the description
- ✅ Keep description **under 72 characters**
- ✅ Use the **body** to explain *why*, not *what*
- ❌ Don't combine unrelated changes in one commit
- ❌ Don't use `fix` for new features or `feat` for bug fixes
- ❌ `style` is **not** for CSS — it's for code formatting

---

## `.releaserc` — What triggers a release

By default `semantic-release` only releases on `feat`, `fix`, and `perf`. To also release on `refactor`, `build`, etc., configure your `.releaserc`:

```json
{
  "plugins": [
    ["@semantic-release/commit-analyzer", {
      "releaseRules": [
        { "type": "refactor", "release": "patch" },
        { "type": "build",    "release": "patch" },
        { "type": "chore",    "release": false }
      ]
    }]
  ]
}
```

---

## At a Glance

```
feat      →  MINOR   new feature
fix       →  PATCH   bug fix
perf      →  PATCH   performance
revert    →  PATCH   undo commit
docs      →  —       documentation
style     →  —       formatting
refactor  →  —       restructure
test      →  —       tests
build     →  —       build tooling
ci        →  —       CI/CD config
chore     →  —       maintenance
feat!     →  MAJOR   breaking change
```