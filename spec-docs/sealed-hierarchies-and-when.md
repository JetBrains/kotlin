# Sealed Class Hierarchies and when-expressions

Goals:
* Make sealed class hierarchies possible (this implies more exhaustiveness criteria for `when`-expressions)

## Discussion

### Sealed Class Option

**This option is chosen as final.**

Special `sealed` keyword is introduced to mark base classes for sealed hierarchies, like this:

``` kotlin
sealed class Type () {
    class Named(val name: String) : Type()
    class Nested {
        class Function(val param: Type, val result: Type): Type()
    }
    object Top: Type()
}
// ...
when (type) {
    is Named -> println(name)
    is Nested.Function -> println("$param -> $result")
    // Alternatively, we can omit is here
    is Top -> println("TOP")
}
```

It's assumed here that sealed class can be subclassed only by nested classes or objects, both with any level of nesting,
but not by local classes of any sort. So the `when` example would operate correctly.

Sealed classes are abstract by default, so abstract modifier is redundant 
(otherwise `when` requires additional case covering all other cases: is Type). 
Sealed classes can never be open or final.
Sealed interfaces are prohibited, otherwise Java classes could easily inherit them.
Sealed objects are also not possible, because we cannot inherit from object.

### Private Constructor Option

Another option would be to make `when` understand hierarchies like this one:

``` kotlin
abstract class Type private () {
    class Named(val name: String) : Type()
    class Function(val param: Type, val result: Type): Type()
    object Top: Type()
}
```

When the superclass has a private constructor, all its subclasses are known to be (directly or indirectly) nested into in, 
and we can be sure that a set of `is`-checks is exhaustive if all the subclasses are included:

``` kotlin
when (type) {
    is Named -> println(name)
    is Function -> println("$param -> $result")
    is Top -> println("TOP")
}
```

However, class with a private constructor can also be derived as a local class, that provides a problem for this option implementation.

## Future When Optimization

It's possible to optimize when on sealed in the way like when on enum or when on string. 
There are the following opportunities, all of them use `KSealed` interface with some `final` identification method:

* use `ordinal()` method, which is implemented like enums, so the first descendant has an ordinal 0, 
second 1 and so on. When on sealed organized like when on enum. Drawback: reordering breaks client's code.
* use `sealedName()` method returning a fully qualified class name of a direct sealed descendant.
When on sealed organized like when on string. Drawback: extra efforts.
* use `sealedId()` method returning a hash code of a direct sealed descendant fully qualified class name.
Drawbacks: possible collisions, including an opportunity to rename some member and get hash code of another member,
which breaks client's code in a dramatic way.

After optimization, `instanceof KSealed` should be checked at run-time before applying it.
