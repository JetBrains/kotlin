# Auto Code Review

This module implements a tool that automatically reviews code changes by running Claude Code
and instructing it to check the changed files against rules defined in `code-rules.md` files.

## Requirements

Install Claude Code CLI and log in: https://code.claude.com/docs/en/quickstart.

## Usage

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

## Operation

The review tool runs Claude Code in [bare mode](https://code.claude.com/docs/en/headless#start-faster-with-bare-mode),
passing the rule and the diff of the affected files as input.
It runs invocations in parallel.

While having a separate session per each rule probably doesn't scale perfectly, it is the easiest way to start with
this rule-based AI code review.
It also makes sure that each invocation is focused on a single rule and receives only the relevant diff.

As a side effect, this approach also allows estimating the API costs for each rule separately
(this information is included in the report).

## code-rules.md format

Rules are organized in special Markdown files named `code-rules.md`.
They work akin to `.gitignore` files: each `code-rules.md` covers files inside its directory.

Here is an example of the file:
```markdown
@../foo/code-rules.md

@/bar/baz.md

# Rule 1 Name

Pattern: *.kt

Pattern: !test

Rule 1 description

# Rule 2 Name

Pattern: test

Rule 2 Description
```

### Include directives

So, at the beginning of the file, there are optional include directives that start with `@`.

* `@../foo/code-rules.md` uses relative path, so the path is resolved as a relative path from the directory the current file is in.
* `@/bar/baz.md` uses "absolute" path, which is in fact resolved as relative from the root of the repository.

The include directive includes the rules defined in the included file and also all same-named files in its enclosing directories.

Note that the file name in the include directive is not required to be `code-rules.md`. So, including `@/foo/bar/baz.md`
adds rules from `$repo/foo/bar/baz.md`, `$repo/foo/baz.md` and `$repo/baz.md`.

The includes are transitive.

### File patterns

Apart from name and description, each rule can have optional file patterns.
Only files matching those patterns will be checked.
The pattern syntax follows [`.gitignore` syntax](https://git-scm.com/docs/gitignore).
But don't be confused: the patterns in code rules files list which files are checked and not which files are ignored.

To include or exclude a file, it is enough to have a single matching pattern.
An exclusion pattern can be defined using `!`. In other words,

```markdown
Pattern: *.kt

Pattern: !test
```

means that the rule applies to all Kotlin files except those inside directories named `test`.
The matching process starts from the last pattern and goes backwards, just like in `.gitignore`.

When deciding whether a rule applies to a file, the tool checks two paths against the patterns:
* If the file is inside the rule file directory, the relative path from that directory to the file is checked.
  This follows the `.gitignore` convention.
* Also, a relative path from the repo root is always checked against the same patterns.
  This allows using patterns with includes: in such a case, some covered files can be outside the rule directory,
  and we need a way to use patterns for them.

For a file to be covered by the rule, it is enough that at least one of those paths matches the patterns.
