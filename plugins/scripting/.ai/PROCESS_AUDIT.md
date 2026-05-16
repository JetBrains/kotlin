# Scripting/REPL — Process Audit Playbook

> **When to consult**: periodically (every ~10 iterations OR every 4 weeks OR whenever the cadence feels off) to evaluate whether the agentic workflow is still pulling its weight. Not for daily work.
> **Cache lifetime**: stable (playbook body); audit findings go into `iterations/audit_YYYY-MM-DD.md`.
> **Last verified**: 2026-05-16

This is a self-audit. Read top-to-bottom, run each recipe, fill the verdict template at the end, commit findings as a dated audit entry.

---

## When to run

| Trigger | What it tells you |
|---|---|
| **Every 10 iterations** | Process velocity + matrix accuracy |
| **Every 4 weeks** | Doc rot, stale Q*, cache trends |
| **After a step regressed twice in a row** | Loadout matrix is wrong for that task type |
| **Cost per iteration trending up** | Cache invalidation or over-loading docs |
| **`ITERATION_RESULTS.md` index > 500 lines** | Archive cadence breached |

If none of these apply: skip. Don't audit by calendar alone — wait until a trigger fires.

---

## 0. Snapshot the state

Run once at audit start. All recipes assume `.ai/` directory:

```bash
cd /Users/ich-jb/Work/kotlin/ws/scripting/plugins/scripting/.ai
AUDIT_TMP="/tmp/scr_audit_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$AUDIT_TMP"
echo "AUDIT_TMP=$AUDIT_TMP"
```

Record git SHA for reproducibility:

```bash
git -C /Users/ich-jb/Work/kotlin/ws/scripting log -1 --format='%H %s' > "$AUDIT_TMP/sha.txt"
cat "$AUDIT_TMP/sha.txt"
```

---

## 1. Reading list (in order)

Load **only** these for the audit — keep the cache prefix small:

1. `AGENT_INSTRUCTIONS.md`
2. `ITERATION_RESULTS.md` + last 5 entries from `iterations/`
3. `target/50-migration-plan.md` (step strike-throughs)
4. `target/90-open-questions.md` (Q* statuses)
5. The audit log directory `iterations/audit_*.md` if any (prior audits)

Skip `current/*.md`, `target/{00,10,20,30,40}-*.md` unless a metric below points at them.

---

## 2. Quantitative metrics

Run each recipe; save output under `$AUDIT_TMP/`.

### 2.1 Iteration velocity

```bash
# Count of landed iterations and date range.
ls iterations/ 2>/dev/null | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}_' | sort > "$AUDIT_TMP/iter_files.txt"
total=$(wc -l < "$AUDIT_TMP/iter_files.txt")
first=$(head -1 "$AUDIT_TMP/iter_files.txt" | cut -d_ -f1)
last=$(tail -1 "$AUDIT_TMP/iter_files.txt" | cut -d_ -f1)
echo "iterations=$total, first=$first, last=$last" | tee "$AUDIT_TMP/velocity.txt"
```

Compute days between first and last, then iterations per week.

**Healthy band**: 1–3 iterations / week if actively working. Below 0.5/week → process is too heavy or work is blocked. Above 5/week → entries may be skimmed (check Key Learnings quality below).

### 2.2 Step landing rate

```bash
# Stricken steps in migration plan.
grep -cE '^### [0-9]+\. ~~' target/50-migration-plan.md > "$AUDIT_TMP/steps_landed.txt"
# Total numbered steps.
grep -cE '^### [0-9]+\.' target/50-migration-plan.md >> "$AUDIT_TMP/steps_landed.txt"
cat "$AUDIT_TMP/steps_landed.txt"
```

First line = landed, second = total. Ratio < 0.1 after several months → sequencing is too aggressive (some "independent" steps are actually blocked). Ratio > 0.7 → close to done; consider closing out the workstream.

### 2.3 Iteration ↔ step mapping

```bash
# Every landed step should have at least one matching iteration entry.
for n in $(grep -oE '^### [0-9]+\.' target/50-migration-plan.md | grep -oE '[0-9]+'); do
  hits=$(grep -lE "step[- ]$n\b|migration[- ]step $n\b" iterations/*.md 2>/dev/null | wc -l)
  marker=$(grep -E "^### $n\. ~~" target/50-migration-plan.md > /dev/null && echo "landed" || echo "open")
  echo "step $n: $marker, $hits iteration(s)"
done | tee "$AUDIT_TMP/step_iter_map.txt"
```

Look for `landed, 0 iteration(s)` → step strike-through without an iteration entry. Post-iteration checklist broke. Reinstate the entry from `git log` + `iterations/` audit.

### 2.4 Test-suite green rate

```bash
# Iterations where any suite went red (After column has any "fail" > 0).
grep -lE '[1-9][0-9]* fail' iterations/[0-9]*.md 2>/dev/null | wc -l > "$AUDIT_TMP/red_iters.txt"
cat "$AUDIT_TMP/red_iters.txt"
```

Any non-zero count → check whether those iterations strike-thru the corresponding step (they shouldn't have). `scripting-step-runner` skill's "stop on first regression" rule was violated.

### 2.5 Open-question aging

```bash
# Q*s with Last touched older than 60 days.
today=$(date +%s)
awk -v today="$today" '
  /^## Q[0-9]+\./ { inblock=1; qid=$2; next }
  inblock && /^- Last touched: / {
    cmd = "date -j -f %Y-%m-%d \"" $4 "\" +%s 2>/dev/null"
    cmd | getline ts; close(cmd)
    if (ts > 0 && (today - ts) > 60*86400) {
      printf "stale: %s (last touched %s)\n", qid, $4
    }
    inblock=0
  }
' target/90-open-questions.md | tee "$AUDIT_TMP/stale_q.txt"
```

Any stale Q* with status `open` or `in-design` → reassess. Either resolve, demote to `blocked`, or accept and update Last-touched as a deliberate "still relevant".

### 2.6 Doc bloat trend

```bash
wc -l AGENT_INSTRUCTIONS.md current/*.md target/*.md ITERATION_RESULTS.md ITERATION_TEMPLATE.md | tee "$AUDIT_TMP/wc.txt"
```

Compare with prior audit's `wc.txt` (if any). Net increase > 20% / quarter → consider another compression pass (rerun Section 2 of the original plan).

### 2.7 Cross-reference rot

```bash
# Every markdown link target must resolve.
{
  for f in AGENT_INSTRUCTIONS.md ITERATION_RESULTS.md ITERATION_TEMPLATE.md current/*.md target/*.md; do
    grep -nE '\]\(([^)]+)\)' "$f" | while read -r line; do
      link=$(echo "$line" | grep -oE '\]\([^)]+\)' | tr -d '])' | sed 's|^(||')
      case "$link" in
        http*|mailto*) continue ;;
        \#*) continue ;;
      esac
      target=$(dirname "$f")/$link
      target="${target%%#*}"
      [ -f "$target" ] || echo "MISSING in $f: $link"
    done
  done
} > "$AUDIT_TMP/broken_links.txt"
wc -l "$AUDIT_TMP/broken_links.txt"
```

Any output → cross-ref consolidation rotted. Fix in the same audit pass.

### 2.8 KT-83498 + bindings-gap reference count

```bash
grep -rcE 'KT-83498' . --include='*.md' | grep -v ':0$' | sort -t: -k2 -rn | head -10 | tee "$AUDIT_TMP/kt83498_refs.txt"
grep -rcE 'bindings.*gap|binding.*gap|bindings injection' . --include='*.md' | grep -v ':0$' | sort -t: -k2 -rn | head -10 >> "$AUDIT_TMP/kt83498_refs.txt"
```

Spec from the plan: KT-83498 canonical home is `target/50-migration-plan.md` step 2 + `current/10-compiler-representation.md` for line anchors. If any other doc has > 1 prose mention (1 link is fine), the consolidation rotted.

### 2.9 Cost per iteration (from per-iteration entries)

Per-iteration entries carry a "Resources & Cost" section (per `ITERATION_TEMPLATE.md`). Aggregate over the audit window:

```bash
# Extract cost USD from every iteration entry since the last audit.
# (uses `find` to avoid shell glob expansion issues when iterations/ is empty)
find iterations -maxdepth 1 -name '[0-9]*.md' -print 2>/dev/null | while read -r f; do
  cost=$(grep -E '^\| Cost \(USD' "$f" 2>/dev/null | head -1 | grep -oE '\$[0-9]+\.[0-9]+' | tr -d '$' || true)
  cache=$(grep -E '^\| Cache hit rate' "$f" 2>/dev/null | head -1 | grep -oE '[0-9]+\.?[0-9]*%' || true)
  echo "$f  cost=${cost:-n/a}  cache=${cache:-n/a}"
done | tee "$AUDIT_TMP/cost_per_iter.txt"

# Mean + max cost across the window.
awk -F'cost=' '/cost=/ { gsub(" .*","",$2); if ($2 != "n/a" && $2 != "") { s += $2; n++; if ($2+0 > max) max=$2 } } END { if (n > 0) printf "mean=$%.4f  max=$%.4f  n=%d\n", s/n, max, n; else printf "no cost data yet\n" }' "$AUDIT_TMP/cost_per_iter.txt" | tee -a "$AUDIT_TMP/cost_per_iter.txt"
```

Compare mean cost / iteration with prior audit's number. Rising cost + flat output → loadout matrix is over-pulling docs. Falling cache hit % → mutable docs are growing.

**Fallback** if entries skip Resources & Cost (audit data missing): scrape session JSONL directly per audit window:

```bash
ls -t ~/.claude/projects/*"$(echo "$PWD" | sed 's|/|-|g')"*/sessions/*.jsonl 2>/dev/null | head -20 | while read -r s; do
  /Users/ich-jb/Work/kotlin/ws/scripting/.claude/scripts/iter-metrics.sh "$s" 2>/dev/null | grep -E '^\| Cost|^\| Cache'
done | tee "$AUDIT_TMP/cost_fallback.txt"
```

If you used the fallback, log a Decision: "tighten `scripting-iteration` skill — entries N…M missed Resources & Cost".

### 2.10 Subagent dispatch correctness (from per-iteration entries)

Aggregate subagent breakdown across the audit window:

```bash
# Sum lines like "  - cavecrew-investigator: N" across all iterations since last audit.
find iterations -maxdepth 1 -name '[0-9]*.md' -print 2>/dev/null | \
  xargs grep -hE '^  - (cavecrew|Explore|Plan|general-purpose)' 2>/dev/null | \
  awk -F': ' '{ gsub(/^  - /,"",$1); s[$1] += $2 } END { for (k in s) printf "%s: %d\n", k, s[k]; if (length(s)==0) printf "no subagent data yet\n" }' | \
  sort -t: -k2 -rn | tee "$AUDIT_TMP/subagent_mix.txt"
```

Expected: `cavecrew-investigator` highest (per dispatch rules), `cavecrew-builder` next, `Explore` last (only > 3-file searches). If `general-purpose` is in top 3, the matrix isn't being followed — refine task descriptions or the matrix.

Also scan "Loadout-vs-actual" blocks for explicit "no" / "over" notes:

```bash
find iterations -maxdepth 1 -name '[0-9]*.md' -print 2>/dev/null | \
  xargs grep -B1 -E '^- Budget hit / over / under: (over|under)' 2>/dev/null | tee "$AUDIT_TMP/budget_miss.txt"
find iterations -maxdepth 1 -name '[0-9]*.md' -print 2>/dev/null | \
  xargs grep -E '^- Subagent dispatch followed: no' 2>/dev/null | tee -a "$AUDIT_TMP/budget_miss.txt"
```

Any hit → Decision: update the matrix row or dispatch rule for that scenario.

---

## 3. Qualitative review

For each item below, answer in the audit entry (Section 5).

### 3.1 Loadout matrix accuracy

Pick 3 recent iterations at random. Read their entries. For each:
- Which task type from the Per-Task Agent Loadout matrix did it match?
- Were the docs listed in "Core docs" actually loaded? (Check the iteration's Changes/Files Modified columns for references.)
- Were extra docs loaded that weren't listed? (Implies matrix row should expand.)
- Was the recommended model used? (Iteration body usually mentions if Opus/Haiku/Sonnet.)

Output: matrix row → "fits / under-spec / over-spec / unused", with one-sentence reason each.

### 3.2 Subagent dispatch correctness

For the same 3 iterations:
- Did cross-module changes go through `cavecrew-investigator` first? (Hard rule.)
- Were 1–2 file edits done via `cavecrew-builder`?
- Were `cavecrew-reviewer` outputs reflected in the diff before commit?

Any "no" → tighten the agent-dispatch rules section in `AGENT_INSTRUCTIONS.md` with the specific scenario that was missed.

### 3.3 Iteration entry quality

For the same 3 iterations:
- Key Learnings field — is it substantive or "no learnings"?
- Test Results — are pass/fail counts real (cross-check against the corresponding `$SCRIPTING_TMP/*.txt` if archived) or `n/a`?
- Post-iteration checklist — boxes ticked or empty?

Any chronic emptiness → the `scripting-iteration` skill isn't being invoked properly, or the template is too heavy. Decide which.

### 3.4 Open-question hygiene

For Q* with status `in-design` or `open` and Last-touched > 30 days:
- Is the question still relevant?
- Has the resolution effectively landed already (without flipping the status)?
- Should it be split into sub-questions?

Apply `scripting-q-resolver` skill to any that should flip.

### 3.5 Cross-doc consistency

Pick one piece of consensus that's referenced in multiple docs (e.g., "Option D recommended for JSR-223 bindings"). Grep for it:

```bash
grep -rl "Option D" --include='*.md' .
```

Read each hit. They should all reference the same canonical home (`target/40-jsr223-target.md`) or strike-thrus on it. If wording diverges, the canonical-home rule rotted.

---

## 4. Common intervention recipes

Given findings, the typical fixes:

| Symptom | Intervention | Files to touch |
|---|---|---|
| Loadout row not loaded → docs missing from iterations | Add docs to the matrix row OR fix the wrong row mapping in `AGENT_INSTRUCTIONS.md` | `AGENT_INSTRUCTIONS.md` Per-Task Agent Loadout |
| Over-load → cost rising | Trim "Core docs" column to minimum; promote optional docs that are always loaded | `AGENT_INSTRUCTIONS.md` Per-Task Agent Loadout |
| `general-purpose` over-used | Add specific dispatch rule for that scenario; tighten cavecrew descriptions | `AGENT_INSTRUCTIONS.md` Agent Dispatch |
| Step landed, no iteration entry | Backfill from `git log` and source files. Surface why the skill didn't gate. | `iterations/`, `scripting-iteration` SKILL.md |
| Q* stale > 60 days | Resolve or update Last-touched. If `in-design` perpetual: convert to a tracked workstream + new YT issue. | `target/90-open-questions.md` |
| Prose bloat > 20% / quarter | Re-run Section 2 of original plan (prose compression). Audit duplicate paragraphs. | `AGENT_INSTRUCTIONS.md`, `target/*.md`, `current/*.md` |
| Broken cross-ref | Fix path. Add to a script: validate links in CI hook. | various |
| KT-83498 / bindings-gap rot | Re-consolidate per original plan Section 3. | `target/50` step 2 + `current/10` |
| Iteration log > 500 lines / 20 entries / 30 days | Run archive procedure (per `ITERATION_RESULTS.md` "Archive cadence"). | `ITERATION_RESULTS.md`, `archive/` |
| `scripting-step-runner` ignoring "stop on first regression" | Tighten the skill body. Add a test recipe that simulates a fail. | `.claude/skills/scripting-step-runner/SKILL.md` |
| Cache hit % falling | Identify which file is rewritten too often (compare wc.txt across audits). Split or stabilize that file. | identified file |

---

## 5. Audit entry template

Save as `iterations/audit_YYYY-MM-DD.md`. Append one-line index entry to `ITERATION_RESULTS.md` with prefix `- YYYY-MM-DD — [process-audit] ...`.

````markdown
# Process audit — YYYY-MM-DD

## Trigger

Which trigger from Section 0 fired. (Calendar / iteration-count / regression streak / cost trend / log overflow.)

## Snapshot

- Git SHA: <from $AUDIT_TMP/sha.txt>
- Iterations since last audit: N
- Steps landed since last audit: N (e.g. step 3, step 7)
- Today's open Q-count: N

## Metrics

Paste relevant tables from `$AUDIT_TMP/*.txt`. Trim ruthlessly — only what changed meaningfully since the prior audit.

| Metric | This audit | Prior | Δ |
|---|---|---|---|
| Iterations / week | ... | ... | ... |
| Steps landed (cumulative) | N / 14 | N' / 14 | + |
| Open-Q count | ... | ... | ... |
| Stale Q* (> 60 days) | ... | ... | ... |
| Total doc lines | ... | ... | ... |
| Broken cross-refs | ... | ... | ... |
| KT-83498 prose mentions | ... | ... | ... |
| Mean cost / iter (USD, from per-iter "Resources & Cost") | ... | ... | ... |
| Max cost / iter (USD) | ... | ... | ... |
| Mean cache hit % | ... | ... | ... |
| Subagent mix (top 3 across window) | ... | ... | ... |
| Iterations with Budget over / under | over=N, under=N | ... | ... |
| Iterations with "Subagent dispatch followed: no" | N | ... | ... |
| Iterations missing Resources & Cost section | N | ... | ... |

## Qualitative findings

- **Loadout matrix accuracy**: ...
- **Subagent dispatch**: ...
- **Entry quality**: ...
- **Open-Q hygiene**: ...
- **Cross-doc consistency**: ...

## Decisions

Bullet list. Each decision = one action item with file path + summary of edit.

- [ ] Edit `AGENT_INSTRUCTIONS.md` Per-Task row "X" → add doc Y.
- [ ] Resolve Q5b in `target/90-open-questions.md` — landed in iteration `iterations/.../`
- [ ] Archive iterations older than YYYY-MM-DD.
- [ ] ...

## Carry-forward

Items deferred to next audit. Surface here so they don't get lost.

- ...

## Next audit trigger

Date or condition. (e.g. "after step 4 lands" or "2026-06-13 calendar")
````

---

## 6. After-audit

1. Apply every "Decision" above as a separate git change (one per file or one per concept). Each gets its own iteration entry only if the change is functional; doc-only audits can be a single commit with no iteration entry needed (this audit IS the entry).
2. Commit the audit entry under `iterations/audit_YYYY-MM-DD.md`. Strike the previous audit's "Carry-forward" items that are now resolved.
3. Update `ITERATION_TEMPLATE.md` checklist if any new gate emerged.
4. If matrix row or dispatch rule changed: bump `AGENT_INSTRUCTIONS.md` "Last updated" footer date.

---

## Caveats

- Audits cost real time (~30–60 minutes including running recipes + writing findings). Don't run more often than the trigger conditions warrant.
- Recipes assume macOS `date` (`date -j -f`). On Linux substitute `date -d`.
- Section 2.10 (subagent mix) depends on `~/.claude/projects/<repo-encoded>/sessions/*.jsonl` paths — adjust if Claude Code session storage moves.
- Do not delete prior audit entries. They're the only historical record of process evolution. Strike-through old "Carry-forward" items in place.
