---
path: "/picking-commits-from-kotlin-to-kotlin-ide"
title: "How to pick commits from Kotlin repo to new kotlin-ide repo"
order: 2
---
# How to pick commits from Kotlin repo to new kotlin-ide repo

`kotlin-ide` new repo doesn't have bunches and uses branches instead (we are not planning doing active backporting anymore). + `kotlin-ide` has completely different layout structure. So in order to address two of these problems special tool for picking commits were created.

1. Downalod `bunch` tool from here: https://github.com/nikitabobko/bunches/releases. This tool is like regular `bunch` tool but with one more command - `cpfe` (cherry pick from external)
2. In `kotlin-ide` repo add Kotlin repo as remote: `git remote add compiler git@github.com:JetBrains/kotlin.git`
3. Fetch from all of your remotes: `git fetch --all`
3. Syntax is: `bunch cpfe sinceCommit untilCommit bunch`. Example: `bunch cpfe a2040f01bfd c7a37eb6b28 202` (Don't see any reasons why you may want to pick from non 202 bunch)

* `sinceCommit` is exclusive, `untilCommit` is inclusive - just regular git notation
* Cherry-pick tool understands bunches format and deals with it. So if there is a rule `202_201` in `.bunch` file, you're picking `202` bunch, and there's `main.kt.201` file without `202` bunch version then tool will automatically detect such cases and will pick changes related to changes in `201` file. So don't worry about that
* You're allowed to `Ctrl+C` cpfe process (it's useful when commit range is big or something went wrong, and you want to amend commit you picked three steps ago). After that  just clean your working directory (`git reset --hard HEAD && git clean -fd` or whatever way you used to clean your worktree) and continue picking commits from where you ended previous time (`cpfe` will check that constraint for you, no worries). There are also other constraints which should be followed when you Ctrl+C cpfe, no worries `cpfe` will guide you.

That's it. Then you may certainly face with conflicts but `bunch` tool will guide you as well (just read all it's messages carefully and follow them)

Pick whatever you need. No need to manually notify anyone about commits you picked. During next big pick commit iteration we will exclude all commits which were picked by you.

This tool may only pick commits from Kotlin repo to `kotlin-ide` repo. Picking commits back from `kotlin-ide` to Kotlin repo isn't supported yet. We will implement this when we will understand that we certainly need it.

Write into #kotlin-ide-internal or #kotlin-20202 in case you have any further questions
