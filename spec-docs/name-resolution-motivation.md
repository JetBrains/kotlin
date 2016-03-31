## Motivation

#### Locals have the highest priority

A variable of function type goes before members:
 
```
class A { fun foo() = 1 }

fun test(a: A, foo: () -> Int) {
    with (a) {
        foo()
    }
}
```

In anonymous objects local variables are chosen, not members:

```
interface A {
    val foo: Int
}

fun createA(foo: Int) = object : A {
    override val foo = foo
}
```

#### Top-level scope chain
 
The priorities: explicit imports; functions in the same package; star-imports; function from stdlib.

Explicit import should hide descriptors imported by `*`.

There is no scope for file, because moving a function to another file in the same package should not change the resolution.

The function imported explicitly goes before the function from the same package; the latter one may live in another file.

#### The order of implicit receivers

See the discussion here: https://youtrack.jetbrains.com/issue/KT-10510.

## Technical notes
 
When we resolve a property `foo` for a call `foo()` we don't stop on the first property, but instead we collect all variables with the name `foo` and for each of them try to find the `invoke` function:
 
``` 
class A {
    val foo: () -> Unit = { println("Hello world!") }
}

fun test(foo: Int) {
    with(A()) {
        foo   // parameter
        foo() // property + invoke
    }
}
```