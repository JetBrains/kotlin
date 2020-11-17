# Code style for C++

**TODO**: Expand beyond naming and formatting

## Headers

* Headers should live in the same folder with it's implementation counterpart (if there's one) **TODO**: This does not work with multiple implementations of a single header.
* Headers should use header guards

## Namespace usage

* Do not use namespace blocks inside implementation files (`*.c`, `*.cpp`, `*.m`, `*.mm`). Use fully qualified names for definitions.
* Do not use `extern "C"` blocks inside implementation files.

### Runtime-specific

* Put main module inside `namespace kotlin`
* Put other modules under `namespace kotlin` in a nested `namespace [module_name]`
* Put implementation details inside `.h`/`.hpp` into a nested `namespace internal` (e.g. implementation details of module `mm` go into `namespace kotlin { namespace mm { namespace internal { ... } } }`)
* Put implementation details inside `.cpp`/`.mm` into a global anonymous `namespace`
* For `extern "C"` declarations emulate namespaces with `Kotlin_[module_name]_` prefixes.

## Naming

* Types should use `PascalCase`
* Local variables and function parameters should use `camelCase`
* Global variables should use `camelCase`
* Constants should use `kPascalCase` (with prefix `k`)
* Private functions (not visible outside a compilation unit) should use `camelCase`
* Exported functions (declared in headers or shared with Kotlin) should use `PascalCase`
* Member fields should use `camelCase`. Private member fields should add `_` suffix: `camelCase_`
* Member functions should use `camelCase`
* Macros should use `SCREAMING_SNAKE_CASE`
* namespaces should use `snake_case`
* `enum` and `enum class` members should use `kPascalCase`

If API is designed to mimic C++ stdlib (e.g. stubbing `<atomic>` for platforms that do not support it), its allowed
to follow stdlib naming conventions.

## Formatting

For automated formatting you can use [config for CLion](codestyle/cpp/CLionFormat.xml) or `clang-format` (see [config](.clang-format) at the repo's root). Note, that CLion uses `clang-format` by default; this can be turned off if you prefer to use rules from `CLionFormat.xml`.

Formatting rules are designed to closely mirror [Kotlin rules](https://kotlinlang.org/docs/reference/coding-conventions.html).

* Use spaces instead of tabs. Indentation width is 4 spaces. Continuation width is 8 spaces
* Do not indent namespaces
* Visibility modifiers are placed without indentation
* All operators should be wrapped with a space or a line break
* In pointer and reference definitions `*` and `&` should be placed on a type instead of a variable
* In pointer to functions prefer not to use `*` at all.
* Add a space between `template` and `<`
