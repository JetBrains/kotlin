# Uniqueness in Kotlin

A key difficulty of formally verifying programs that make
use of the heap is *aliasing*.
This topic is not new, with [The Geneva Convention on the
Treatment of Object Aliasing][1] (1991) giving an outline of
this problem that is still relevant today.
In brief, when two separate references may refer to the same
object, we cannot assume that a operations on one do not
modify the other.  For example, we cannot simplify

```kt
x.a += y.a
x.a += y.a
```

to `x.a += 2 * y.a`, since if `x = y`, the second use of
`y.a` has a different value than the first.
Writing and verifying a specification for this kind of code
is challenging.

Kotlin, being an object oriented language, uses the heap
extensively.
Without some way to constrain aliasing, few programs can be
verified.

Kotlin is also a concurrent language, making our job even
harder.
In the following example, we could conclude that the smart
cast is safe in a single-threaded context, but Kotlin does
not permit it:

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

There is a rich body of literature on annotation systems
for controlling aliasing, much of it collected in the book
[Aliasing in Object-Oriented Programming][5].
For this plugin, we introduce a system based on the [Alias
Burying][2] system by Boyland.
While our primary motivation is formal verification, we
think such a system can also be useful for other forms of
program analysis, memory reuse, and can enrich the smart
cast rules of the language itself.

## Our system

### Theoretical part

The [Geneva Convention][1] defines aliasing as a single
object being reachable via multiple paths in some context.
A context is a set of object references, and a path to an
object is an expression that resolves to that object.

In this sense, the context of a method includes:
* Method parameters, including `this`
* (Accessible) global variables
* Local variables
* Anonymously constructed objects
* Return values of called methods

These form the roots of paths, which are built up from
method references in the context and from property accesses.

We say two paths alias if they refer to the same object.

Consider the following example:

```kt
class X(val a: Any?)

fun f(x: X, y: X) {
    // context: x, y
    // paths: x, y, x.a, y.a
}
```

To say that `x` is not aliased in this context means to say
that `x` does not refers to the same object as any of `y`,
`x.a`, or `y.a`.

This is a good initial understanding, but we are interested
in a more nuanced perspective.
In addition to the context of the method being verified, we
consider three other contexts:
* The parent context is the context of all methods in the
  call chain above the current method.
* The child contexts are the contexts of all methods in the
  call chain below a current method.  Every method call
  gives a new child context.
* The concurrent context is the set of object references
  accessible to other threads.

With these contexts in mind, we define our two notions,
uniqueness and borrowing.
There is a slight duality here: any value may *become*
borrowed, at which point we are restricted in what we may do
with it, while any unique value may *lose* its uniqueness.

An object reference marked *borrowed* may only be copied to
other object references marked borrowed.
Note that class members and return values may not be marked
borrowed, so the borrowed reference may only travel down the
call graph.

An object reference `p` is *unique* if it is `null` or if
all the following hold:
* It does not alias anything in the current context or in
  the concurrent context.
* Every object reference it aliases in a child context is
  borrowed.

The notion of a borrowed variable is similar to that used in
[Alias Burying][2].
The key difference is that for us, a reference may be
unique, borrowed, both, or none.

### Implementation

We allow annotation of parameters and return values to be
annotated `Unique`, which requires that they satisfy
uniqueness in the sense above.

We allow method parameters to be annotated with `Borrowed`,
which makes the reference borrowed in the sense above and
restricts it in the same sense.
Note that this is a weaker, more generally applicable, form
of the `callsInPlace` contract already present in Kotlin.

Receivers may be annotated in this way as well.
Syntactically, this involves placing the annotation on the
method itself.
(A solution for multiple receivers still needs to be
devised.)


## Open Problems

Our system outlined above is an MVP that we think will allow
some amount of verification, but is not a production-level
system.
For that, we need to tackle a number of further questions.

### Checking annotations

[Solving Shape-Analysis Problems in Languages with
Destructive Updating][3] is the approach used by the authors
of [Alias Burying][2] to statically check the correctness of
uniqueness annotations.

We believe that our system can use a similar algorithm, but
have yet to verify this.

### Captured variables

Our discussion of contexts and paths does not clearly define
the role of variables captured by lambdas.
The key question here is whether a variable can be captured
and still retain uniqueness.

### Annotation inference

[Existing literature][4] shows that in systems similar to
ours, annotations on fields, return values and parameters
are enough to infer annotation for local variable
declarations.
This would be convenient, but it is unclear whether it is
possible in all cases.

### Class property annotations

Our system is for now restricted to annotations on method
parameters (including `this`), return values, and local
variables.
Alias Burying also specifies semantics for annotations on
class properties, but we are not sure that these semantics
are a good fit for Kotlin.

The following example illustrates how such annotations might
be used.
We omit annotations on local variables to avoid clutter.

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

In this example, both `Node` and `Stack` can only
meaningfully be used uniquely.
One possibility is to require annotating the class itself
with `Unique`, which would make the semantics clearer.
However, it is not the only option: we could say that
modifying the `root` property of a shared instance of
`Stack` is permitted, but only using a `replace` operation
that atomically sets it to new `@Unique Node?`.

The `value` property of `Node` is in this example forced to
be unique, but in practice the user may want a `Stack` to be
polymorphic in the uniqueness of its elements.
It is an interesting question whether this can easily be
supported.


## Benefits of Uniqueness

Uniqueness annotations give the compiler information and
that would otherwise be known only to the programmer.
This also brings benefits to programmers who do not care
about formal verification.

### Smart casts

In Kotlin, smart casts are defined based on a set of [stable
expressions][6].
Properties of objects referred to by a stable unique
reference can themselves be considered stable.
This allows us to accept the following code:

```kt
class X(var n: Int?)

fun useX(x: @Borrowed @Unique X): Int =
    if (x.n != null) {
        x.n // Uniqueness grants that 'x.n' have not changed after checking is nullability
    } else {
        0
    }
```

Note that the `@Unique` annotation permits the smart cast,
while the `@Borrowed` annotation allows the caller of `useX`
to not lose the uniqueness of the argument.

### Analysis

Aliasing in Kotlin also poses a challenge for the static
analyzer used by IntelliJ IDEA.
In the following example, aliasing results in an incorrect
constant condition as provided by the IDE.

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

### In-place modification

When writing in a more functional style, operations on
collections may look something like the following:

```kt
myList.map { ... }.filter { ... }
```

The existing implementations of `map` and `filter` will
create a new list to store the results.
However, if the passed-in list is unique then this is
unnecessary: the list can be modified in place instead.
By providing uniqueness annotations and allowing functions
to be overloaded based on uniqueness, we could define
variants of these standard library functions that reuse the
list, thereby improving performance.

## Connection to Valhalla and immutability

One related development in the Java world is [Project
Valhalla][7], which will bring value types to the JVM.
These are types that do not support object identify: the
user is not given any guarantees on whether two references
refer to the same object, or to two separate objects with
the same values.
Value objects are also required to only have immutable
members.

Due to this immutability, marking a value object as unique
does not have any immediate impact in the system we have
described now: all information available about the values
can be known without uniqueness.
However, if we extend the system to allow uniqueness
annotations on member properties, there may be interactions
between the two systems.


[1]: https://dl.acm.org/doi/pdf/10.1145/130943.130947
[2]: https://onlinelibrary.wiley.com/doi/abs/10.1002/spe.370
[3]: https://dl.acm.org/doi/pdf/10.1145/271510.271517
[4]: https://arxiv.org/pdf/2309.05637.pdf
[5]: https://dl.acm.org/doi/10.5555/2554511
[6]: https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability
[7]: https://openjdk.org/projects/valhalla/
