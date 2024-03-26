# Aliasing problem overview

As described in [The Geneva Convention][1] aliasing between references
can make it difficult to verify simple programs.
Let's consider the following Hoare formula:

_{x = true} y := false {x = true}_

If _x_ and _y_ refer to the same
reference (they are aliased) the formula is not valid. \
Aliasing is not only a problem for formal verification, but also results in mysterious bugs for the programmer, as
variables change their values seemingly on their own.

Once again, according to [The Geneva Convention][1], the set of (object address) values associated with variables during the execution of a
method is a context. It is only
meaningful to speak of aliasing occurring within some context; if two instance variables refer to a single object, but
one of them belongs to an object that cannot be reached from anywhere in the system, then the
aliasing is irrelevant. \
Within any method, objects may be accessed through paths rooted at any of:

- Self
- An anonymous locally constructed object
- A method argument
- A result returned by another method
- A (global) variable accessible from the method scope
- A local method variable bounded to any of the above

An object is **aliased** with respect to some context if two or more such paths to that object exist.

# Aliasing in Kotlin

## Smart casts

The aliasing issue is evident in Kotlin, as the language does not impose any restrictions on aliasing. In the following
example, the compiler is unable to execute a seemingly obvious smart cast from the user's perspective. However, the
compiler's analysis is accurate, as `x` may be aliased, and another thread could potentially modify the `x.n` field.

```kt
class X(var n: Int?)

fun useX(x: X): Int =
    if (x.n != null) {
        x.n
//      ^^^ 
// Smart cast to 'Int' is impossible, because 'x.n' is a mutable property that could have been changed by this time
    } else {
        0
    }
```

## Proving functional behaviour

The previous example illustrates how even basic functional properties can't be validated when aliasing and mutability
are involved. Aliasing in Kotlin also poses a challenge for the static analyzer used by IntelliJ IDEA. In the following
example, aliasing results in an incorrect constant condition as provided by the IDE.

```kt
class A(var a: Boolean = false)

fun f(a1: A, a2: A) {
    a1.a = true
    a2.a = false
    if (!a1.a) {
//      ^^^^^
// Condition '!a1.a' is always false 
        println("ALIASED!")
    }
}

fun main() {
    val a1 = A()
    f(a1, a1) // prints "ALIASED!"
}
```

# Our Uniqueness System

We propose an annotation system able to provide some guarantees on the uniqueness of the variables.

## Uniqueness invariant

A unique variable is one whose value is null or else refers to an unshared object, one referred to by no other
variables. This situation is called the _uniqueness invariant_. Parameters, receivers and return values may be declared
unique. A use of a unique variable is a destructive read: the variable is atomically set to null at the same time that
the value is read. The destruction preserves the _uniqueness invariant_.

## Alias Burying and relaxation of uniqueness invariant

Ensuring the _uniqueness invariant_ requires unique variables to be set to null every time they are
read. [Alias burying][2] keeps the uniqueness invariant but only requires it to be true when it is needed.

The uniqueness invariant, therefore, is not actually true at every point in the program. However, the points when it is false are
‘uninteresting’. That is, a unique variable, once read, is never read again, or it's eventually re-assigned before being read again.

## Annotations sets

- All fields and return values can be annotated as `Unique`. If a field or a return value is not annotated
  with `Unique`, it is considered to be shared.
- Method parameters (including the receiver) can be annotated with one or both of these annotations:
  `Unique`, `Borrowed`. Also in this case, absence of annotations means that the parameter is shared.
- [Existing literature][4] shows that in systems similar to ours, annotations on
  fields, return values and parameters are enough to infer annotation for local variable declarations.

## Annotations meaning

- `Unique` denotes ownership, the value is only stored at this location.
- `Borrowed` method parameter ensures that no further aliases are created by the method.

## Smart casts and uniqueness

Moving back to the [smart cast example](#smart-casts), we can see how our annotation system can help the compiler
performing a smart cast.

```kt
class X(var n: Int?)

fun useX(x: @Unique X): Int =
    if (x.n != null) {
        x.n // Uniqueness grants that 'x.n' have not changed after checking is nullability
    } else {
        0
    }
```

Note that in this example, `useX` doesn't create new aliases of the parameter `x` and so it is possible to annotate it
as `Borrowed`. Annotating parameter `x` as `Borrowed` preserves uniqueness after `useX` returns.

```kt
fun useX(x: @Borrowed @Unique X): Int {
    // ...
}

fun main() {
    val a = X(1) // Uniqueness here can be inferred
    val y = useX(a)
    // since the parameter 'x' in 'useX' is borrowed, 'a' is still unique after the call
    val z = useX(a)
}
```

## Checking annotations

[Solving Shape-Analysis Problems in Languages with Destructive Updating][3] is the approach used by the authors
of [Alias Burying][2] to statically check the correctness of uniqueness annotations.

## Example

```kt
class Node(var value: @Unique Any?, var next: @Unique Node?)

class Stack(var root: @Unique Node?) {
    @Borrowed
    @Unique
    fun push(value: @Unique Any?) {
        val r = this.root
        this.root = null
        val n = Node(value, r)
        this.root = n
    }

    @Borrowed
    @Unique
    fun pop(): @Unique Any? {
        val value: Any?
        if (this.root == null) {
            value = null
        } else {
            value = this.root!!.value // here uniqueness should allow a smart cast
            val next = this.root!!.next // here uniqueness should allow a smart cast
            this.root = next
        }
        return value
    }
}
```

[1]: https://dl.acm.org/doi/pdf/10.1145/130943.130947

[2]: https://onlinelibrary.wiley.com/doi/abs/10.1002/spe.370

[3]: https://dl.acm.org/doi/pdf/10.1145/271510.271517

[4]: https://arxiv.org/pdf/2309.05637.pdf