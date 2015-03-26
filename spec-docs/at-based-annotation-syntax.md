# @-based Syntax For Annotations

Goals:
* Spare `[...]` in expression position for future use
* Support targeting for annotations (`field`, `getter` etc)
* Preserve affected/clashing functionality (`@labels`)

## Examples

Annotations:
``` kotlin
@AnnotationName(args)
class Foo
```

Targeted annotation:
``` kotlin
class C(@field:Foo val x: Int, @field,parameter:Bar val y: Int)
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
