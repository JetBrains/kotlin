# Auto Code Review

This module implements a tool that automatically reviews code changes by running Claude Code
and instructing it to check the changed files against rules defined in `code-rules.md` files.

## Requirements for running locally

Install Claude Code CLI and log in: https://code.claude.com/docs/en/quickstart.

## Run locally

```shell
../../gradlew -q reviewCode
```

You can also use a run configuration named `Review diff with master` in IntelliJ IDEA.

It reviews the diff between the current working tree (including uncommitted changes in tracked files)
and the revision in `origin/master` it is branched from.

If you need to define a different base branch, pass `--base $branch`.
`--base HEAD~5` also works, to review the last five commits.

Upon completion, the tool generates a Markdown-formatted report and prints a link to it.
The report includes rule violations (if any) and some meta-information.

If you run the tool from IntelliJ IDEA, the printed report link is clickable and opens the report in the IDE.
Viewing the report in IntelliJ IDEA with Markdown preview enabled is the intended way of reading it:
all the links to code and code rules are clickable and open the destinations in the IDE.

## Run on CI

JetBrains employees can also run the tool on the CI.

1. Go to [the build configuration](https://kotlinlang.teamcity.com/buildConfiguration/Kotlin_KotlinCloud_Dev_AutoCodeReview).
2. Select the branch to review in the "Branch" area.
3. Press "Run".
4. In the opened dialog, make sure to enter the proper base branch name ("master" by default).
5. Press "Run Build" in the dialog and navigate to the scheduled build.
6. Wait until the build is finished, and read the review report in the "Review" tab of the build.

## Operation

The review tool runs Claude Code in [bare mode](https://code.claude.com/docs/en/headless#start-faster-with-bare-mode),
passing the rule and the diff of the affected files as input.
It runs invocations in parallel.

While having a separate session per each rule probably doesn't scale perfectly, it is the easiest way to start with
this rule-based AI code review.
It also makes sure that each invocation is focused on a single rule and receives only the relevant diff.

As a side effect, this approach also allows estimating the API costs for each rule separately
(this information is included in the report).

## Rule files

Rules are organized in special Markdown files named `code-rules.md`.

Some sanity checks for all the `code-rules.md` files in the repository are implemented as tests in
[RepoCodeRulesTests](test/org/jetbrains/kotlin/code/review/RepoCodeRulesTests.kt).
For example, those tests check that all rule files can be parsed successfully, all includes refer to existing files,
and every rule applies to at least one file.

When updating rule files, it is reasonable to run those tests. The tests are also included to Aggregate.

### code-rules.md format

`code-rules.md` files work akin to `.gitignore` files: each `code-rules.md` covers files inside its directory.

Here is an example of the file:
````markdown
@../foo/code-rules.md

@/bar/baz.md

# Rule 1 Name

Applies to:
```
*.kt
!test
```

Rule 1 description

# Rule 2 Name

Applies to: `test`

Rule 2 Description
````

### Include directives

So, at the beginning of the file, there are optional include directives that start with `@`.

* `@../foo/code-rules.md` uses relative path, so the path is resolved as a relative path from the directory the current file is in.
* `@/bar/baz.md` uses "absolute" path, which is in fact resolved as relative from the root of the repository.

The include directive includes the rules defined in the included file,
as if they were defined in the including file:
the rules apply to files in the including file directory (and its subdirectories),
and their patterns are relative to that directory (see [File patterns](#file-patterns)).
Note that the file name in the include directive is not required to be `code-rules.md`.

Only the included file itself is included, the rule files in its enclosing directories aren't included automatically.
For example, including `@/foo/bar/code-rules.md` doesn't include `/foo/code-rules.md`.

The includes are transitive.

### File patterns

Apart from name and description, each rule can have optional file patterns.
Only files matching those patterns will be checked.

The patterns are defined with the `Applies to:` directive right after the rule name.
A single pattern can be put on the same line, in backticks:

```markdown
Applies to: `test`
```

Any number of patterns can be put into a code block right after the directive, one pattern per line:

````markdown
Applies to:
```
*.kt
!test
```
````

The pattern syntax follows [`.gitignore` syntax](https://git-scm.com/docs/gitignore),
and the patterns are relative to the directory of the `code-rules.md` file.
But don't be confused: the patterns in code rules files list which files are checked and not which files are ignored.

There are two kinds of patterns:

| Kind       | Examples                                  | Matches                                                |
|------------|-------------------------------------------|--------------------------------------------------------|
| Unanchored | `*.kt`, `test`, `test/`, `**/src/**/*.kt` | Paths at any depth                                     |
| Anchored   | `/build.gradle.kts`, `src/main`, `src/**` | Paths relative to the directory of the `code-rules.md` |

A pattern is unanchored if it has no `/` except a trailing one, or if it starts with `**/`.
A pattern matching a directory also matches all files inside it.

An exclusion pattern can be defined using `!`.
Exclusion patterns must go after all other patterns.
A rule applies to a file if the file matches at least one of the regular patterns and none of the exclusion patterns.
For example, the patterns 

````markdown
Applies to:
```
*.kt
!test
```
````

mean that the rule applies to all Kotlin files except those inside directories named `test`.

### Patterns in included rules

The patterns of included rules are relative to the directory of the including file, not of the included one.
So, if a rule is included from another directory, its anchored patterns would have a different meaning there.
To avoid confusion, such rules can have only unanchored patterns, and the tool reports anchored patterns as errors.

For example, if `/foo/code-rules.md` includes `@/bar/code-rules.md`, which has a rule that applies to `src`,
the rule applies to files inside `src` directories in both `/bar` and `/foo`.

As the includes are transitive, the same holds for the rules included indirectly:
their patterns are relative to the directory of the `code-rules.md` that includes them, directly or not.
So, if `/bar/code-rules.md` also includes `@/baz/shared.md`, the rules from `/baz/shared.md` apply to files in `/foo`,
with the patterns relative to `/foo`, and to files in `/bar`, with the patterns relative to `/bar`.

Including a rule file located in the same directory as the `code-rules.md` (e.g. with `@more-rules.md`)
doesn't prohibit anchored patterns in the included file (`more-rules.md`).
