#!/usr/bin/env bash
set -euo pipefail

# Junie rebase helper
# Rebases the current branch onto the base branch (origin/master preferred, origin/main fallback).
# Provides clear diagnostics in restricted environments (e.g., when fetching is limited).

say() { printf "[junie-rebase] %s\n" "$*"; }
err() { printf "[junie-rebase][ERROR] %s\n" "$*" 1>&2; }

current_branch=$(git rev-parse --abbrev-ref HEAD)
tracking_branch=$(git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>/dev/null || true)

say "Current branch: ${current_branch}${tracking_branch:+ (tracking ${tracking_branch})}"

# Determine base ref
base_ref="origin/master"
if ! git show-ref --verify --quiet "refs/remotes/${base_ref}"; then
  # Fallback to origin/main if origin/master doesn't exist locally
  if git show-ref --verify --quiet refs/remotes/origin/main; then
    base_ref="origin/main"
  else
    # Try to detect existence remotely via ls-remote
    if git ls-remote --exit-code --heads origin master >/dev/null 2>&1; then
      base_ref="origin/master"
    elif git ls-remote --exit-code --heads origin main >/dev/null 2>&1; then
      base_ref="origin/main"
    else
      err "Could not detect base branch (master/main) on remote 'origin'."
      exit 2
    fi
  fi
fi

say "Base branch candidate: ${base_ref}"

# Ensure base ref exists locally; attempt to fetch it explicitly
need_fetch=0
if ! git show-ref --verify --quiet "refs/remotes/${base_ref}"; then
  need_fetch=1
fi

if [[ "$need_fetch" -eq 1 ]]; then
  say "Fetching ${base_ref} from origin..."
  # Use full refspec to avoid overfetch; allow environments where tags flood fetch output
  refname=${base_ref#origin/}
  if ! git fetch --prune --no-tags origin "refs/heads/${refname}:refs/remotes/origin/${refname}" 2>/dev/null; then
    # Try shallow fetch
    if ! git fetch --depth=200 --no-tags origin "refs/heads/${refname}:refs/remotes/origin/${refname}" 2>/dev/null; then
      err "Unable to fetch ${base_ref}. This environment may restrict fetching other branches."
      err "Run this script locally or in a CI job with full fetch permissions to complete the rebase."
      exit 3
    fi
  fi
fi

if ! git show-ref --verify --quiet "refs/remotes/${base_ref}"; then
  err "Base ref ${base_ref} is still unavailable after fetch attempts. Aborting."
  exit 4
fi

say "Rebasing ${current_branch} onto ${base_ref}..."
# Rebase with autosquash disabled, preserve merges if any
set +e
GIT_SEQUENCE_EDITOR=:
GIT_COMMITTER_DATE_IS_AUTHOR_DATE=1 \
  git rebase --rebase-merges --no-autosquash "${base_ref}"
status=$?
set -e

if [[ $status -ne 0 ]]; then
  say "Rebase reported non-zero status ($status). Checking for conflicts..."
  if git diff --name-only --diff-filter=U | grep -q .; then
    err "Rebase conflicts detected. Please resolve conflicts and run 'git rebase --continue'."
    git status --porcelain=v1
    exit 5
  else
    err "Rebase failed without detectable conflicts (status $status)."
    exit $status
  fi
fi

say "Rebase completed successfully."

# Optionally advise pushing
if [[ -n "${tracking_branch}" ]]; then
  say "You can push the rebased branch with: git push --force-with-lease"
fi
