# Sealed Class Hierarchies and when-expressions

Goals:
* Make sealed class hierarchies possible (this implies more exhaustiveness criteria for `when`-expressions)

## Discussion

### Private Constructor Option

One option would be to make `when` understand hierarchies like this one:

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

### Sealed Class Option

Another option would be to introduce special `sealed` keyword to mark base classes for sealed hierarchies, like this:

``` kotlin
abstract sealed class Type () {
    class Named(val name: String) : Type()
    class Function(val param: Type, val result: Type): Type()
    object Top: Type()
}
```

It's assumed here that sealed class can be subclassed only by inner classes / objects, but not by local classes of any sort.
So the `when` example above would operate correctly.

