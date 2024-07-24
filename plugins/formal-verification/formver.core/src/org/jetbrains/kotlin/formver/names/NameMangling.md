# Name mangling

There are two hard problems in computer science:
cache invalidation, naming, and off-by-one errors.

When translating Kotlin to Viper, we need to pick distinct names for objects.
Viper is significantly less forgiving than Kotlin when it comes to shadowing,
so we aim to pick unique names globally.

We do this by scoping and typing the names:
* Names are given a scope corresponding (roughly) to their scope in the original
  Kotlin program.
* Names are given a type based on the kind of thing they refer to.

Since this typically makes names long, we use abbreviations, or omit the type
and/or scope for the most common cases.

For scopes:
* `pkg` for package
* `g` for global
* `p` for parameter
* `ln` for local, where `n` is the number of the scope.

For types:
* `c` for class
* `f` for function
* `d` for domain
* `lbl` for label
* `anon` for anonymous value
* `ret` for return value
* `con` for constructor
* `pp` for property (internal use only)
* `bf` for backing field
* `pg` for property getter
* `ps` for property setter
* `eg` for extension getter
* `es` for extension setter
* `p` for predicate

There are still holdover (longer) names.

Ideally, we would like this system to be modular and configurable, dropping
prefixes when they are unused, etc.  However, that is WIP.