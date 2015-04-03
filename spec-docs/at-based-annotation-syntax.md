# @-based Syntax For Annotations

Goals:
* Spare `[...]` in expression position for future use
* Support targeting for annotations (`field`, `getter` etc)
* Preserve affected/clashing functionality (`@labels`)
* Support `open` and other modifiers on local classes

## Examples

Annotation:
``` kotlin
@AnnotationName(args)
class Foo
```

Targeted annotation:
``` kotlin
class C(@field:Foo val x: Int, @field,parameter:Bar val y: Int)
```

another option (like in Scala):
``` kotlin
class C(@(Foo@field) val x: Int, @(Bar@(field,parameter)) val y: Int)
```

yet another option (requires allowing annotation arrays at least in source-retained annotations):

``` kotlin
class C(@field(@Foo) val x: Int, @field(@Bar) @parameter(@Bar) val y: Int)
```

Labels:
``` kotlin
loop@ // declaring a label
for (x in foo) {
    if (x > 0) continue@loop // using a label
}
```

## Syntactic Disambiguation

How can we avoid confusion between `continue@loop` and

``` kotlin
if (foo) continue
@ann val x = 1
```

or `return@label (x + 1) + 5` and `return @ann(x + 1) +5`

Rules:
* no newline allowed between `continue`/`break`/`this`/`super`/`return` and `@label`
* only one label allowed after these keywords, everything after the first label is an annotation
* for `return` we prefer `return@label` to `return @ann expr`, so one should say `return (@ann expr)`

## Targeting 

Possible targets are
* `field`
* `get`
* `set`
* `property`
* `parameter` - for constructor parameters that are also properties

Reasonable defaults would probably be:
* `field` if there's a backing field
* `get` otherwise

Otherwise, determined by the settings of the annotation itself (applicable to fields only -> goes to a field)

### Possible Syntax for Targeting

Special syntax:

``` kotlin
class C(@field:Ann("arg") var foo: Int)
```

This is a rather limited dedicated solution: it is unclear, for example, how you define a new target, also this syntax can not be used for anything else but targeting.

Scala-like syntax:

``` kotlin
class C(@(Ann@field)("arg") var foo: Int)
```

Too many parentheses, but the mechanism is generic.

Annotation-array-based syntax:

``` kotlin
class C(@field(@Ann1("arg"), @Ann2) var foo: Int)
```

Downside: to put the same annotation on two targets, we'd need to duplicate it.

For this, we need to allow annotation attributes of type `Array<Annotation>`:

``` kotlin
annotation class field(vararg val annotations: Annotation)
```
**NOTE**: This is only relatively easilty achievable for source-retained annotations, for class- or runtime-retained it's a lot more involved and relies on an undocumented features of JVM.

Another approach:

``` kotlin
class C(@at(FIELD, @Ann1("arg"), @Ann2) var foo: Int)
class C(@atMany(array(FIELD, PROPERTY), @Ann1("arg"), @Ann2) var foo: Int)
```

Then definitions are as follows:

``` kotlin
annotation class at(val target: AnnotationTarget, vararg val annotations: Annotation)
annotation class atMany(val target: Array<AnnotationTarget>, vararg val annotations: Annotation)
```

Yet another approach involves adding an explicit syntax for declaring fields:

``` kotlin
@Ann1("arg") @Ann2
val foo: Int = 1
    @Ann1("arg") @Ann2
    field _foo
    @GetterAnnotation
    get
```

* There's no way to mitigate duplication here
* It is likely to become an obscure piece of syntax (like `$backingField`) which is used rarely and supported poorly by tools

## Modifiers and Annotations on Local Declarations

Current state is as follows:
``` kotlin
fun example() {
    open abstract final; // binary expression
    class Foo
    
    open class Bar // modifier list
    
    data class Bar // syntax error: add ";" between expression and declaration
}
```

Typical useful cases:
``` kotlin
fun example() {
    open class LocalOpen
    abstract class LocalAbstract
    enum class LocalEnum
    inline fun local() {}
    volatile var local = ...
}
```

Our goals/constraints:
* We don't want to make users remember that `open` is a modifier, but `data` is an annotation => we need to treat both in a **uniform** way (both escaped or both unescaped)
* REPL kind requires a possiblity of writing `open class Foo` in a local context (or we need a special parser tweak for REPL), same for scripts (parser tweak won't work here)
* Allowing annotations on the previous line may reqult in quadratic parsing:
``` kotlin
fun example() {
    foo() // maybe this starts a modifier list?
    bar() // or this?
    baz() // or this?
    goo() // or this?
    // no
}
```

Our options:
* **Leave it as is**: `open` works without escaping, but `data` doesn't (we can add recovery in the parser, so that the error message is comprehensible).

* **Require escaping** for both modifiers and annotations in a local scope:
``` kotlin
fun example() {
    @open @data class Local { ... }
}
```
The same syntax is allowed on all modifier everywhere in Kotlin. 

Downside: **REPL/scripts** won't forgive us.

Then, it makes sense to prohibit naming annotation classes `public`, `open` etc, i.e. like modifiers.

> Alternative: do not prohibit this, but prefer modifiers to annotations, one can use a qalified name to refer to an annotation then: `@my.package.open`

Further development of this direction may involve complete unification of modifiers and annotations.

* **Allow unescaped annotations** *on the same line*:
``` kotlin
fun example() {
    open data class Local { ... } // OK
    open data // syntax error
    class Local2 { ... }
}
```

Downside: This won't so easilty work for functions (which are also valid expressions), so we'd have to either **prohibit annotating local functions**, or **forbid using *named* functions** as rhs arguments of infix calls (`list map fun mapper() { ... }` <- this is an error)

This may be allowed *together* with escaping for modifiers.
