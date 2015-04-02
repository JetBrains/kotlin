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

## Escaping For Modifiers

Since modifiers (being soft-keywords) are not parseable in local declarations (despite the present erroneous behaviuor in our parser), we need to be able to escape modifier names by prefixing them with `@`:

``` kotlin
fun example() {
    @open class Local { ... }
}
```

The same syntax is allowed on all modifier everywhere in Kotlin.

As a consequence, it must be prohibited to name annotation classes `public`, `open` etc, i.e. like modifiers.
