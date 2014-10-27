# Function Types in Kotlin on JVM

## Goals

* Get rid of 23 hardwired physical function classes. The problem with them is,
reflection introduces a few kinds of functions but each of them should be invokable as a normal function as well, and so
we get `{top-level, member, extension, member-extension, local, ...} * 23` = **a lot** of physical classes in the runtime.
* Make extension functions coercible to normal functions (with an extra parameter).
At the moment it's not possible to do `listOfStrings.map(String::length)`
* Allow functions with more than 23 parameters, theoretically any number of parameters (in practice 255 on JVM).
* At the same time, allow to implement Kotlin functions easily from Java: `new Function2() { ... }` and overriding `invoke` only would be the best.
Enabling SAM conversions on Java 8 would also be terrific.

## Brief solution overview

* Treat extension functions almost like non-extension functions with one extra parameter, allowing to use them almost interchangeably.
* Introduce a physical class `Function` and unlimited number of *fictitious* (synthetic) classes `Function0`, `Function1`, ... in the compiler front-end
* On JVM, introduce `Function0`..`Function22`, which are optimized in a certain way,
and `FunctionN` for functions with 23+ parameters.
When passing a lambda to Kotlin from Java, one will need to implement one of these interfaces.
* Also on JVM (under the hood) add abstract `FunctionImpl` which implements all of `Fun0`..`Fun22` and `FunN`
(throwing exceptions), and which knows its arity.
Kotlin lambdas are translated to subclasses of this abstract class, passing the correct arity to the super constructor.
* Provide a way to get arity of an arbitrary `Function` object (pretty straightforward).
* Hack `is/as Function5` on any numbered function in codegen (and probably `KClass.cast()` in reflection) to check against `Function` and its arity.

## Extension functions

Extension function type `T.(P) -> R` is now just a shorthand for `[kotlin.extension] Function2<T, P, R>`.
`kotlin.extension` is a **type annotation** defined in built-ins.
So effectively functions and extension functions now have the same type,
how can we make extension function expressions support extension function call syntax?

We introduce the following convention: expression `foo` of type `Foo` can be used as an extension function
(i.e. `object.foo(arguments)`) if and only if there is a function `invokeExtension`
with the corresponding parameters available on the type `Foo`.
This function may be declared in class `Foo` or somewhere as an extension to `Foo`.

> Note that at the moment a less convenient convention is used: there must be a **member extension**
> function `invoke` in the class which you want to be used as an extension function.
> This means you can't add "extension-function-ness" to a foreign class,
> since you'd need to declare a function with two receivers.
> The new approach will solve this problem.

We declare `invokeExtension` to be available on all extension functions:

``` kotlin
package kotlin

...
fun <T, R> (T.() -> R).invokeExtension(): R = this()
fun <T, P1, R> (T.(P1) -> R).invokeExtension(p1: P1): R = this(p1)
...
```

So now an expression type-checked to an "extension function type" can be used with the desired syntax.
But, since a function type and a corresponding extension function type effectively have the same classifier (e.g. `Function7`),
they are coercible to each other and therefore our `invokeExtension` will be applicable to the usual
functions as well, which is something we don't want to happen! Example:

``` kotlin
val lengthHacked: (String) -> Int = { it.length }

fun test() = "".lengthHacked()  // <-- bad! The declared function accepts a single non-receiver argument
                                // and is not designed to be invoked as an extension
```

And here we introduce the following **restriction**: given a call `object.foo(arguments)`,
if `foo` is resolved **exactly** to the built-in extension function `invokeExtension`,
then the call *will not compile* unless the receiver type is annotated with `[extension]`.
So `invokeExtension` will yield an error when used on a normal (not extension) function.

To make your class invokable as an extension you only need to declare `invokeExtension`.
Declaring `invoke` (and maybe overriding it from the needed function class) will only make your class invokable *as a usual function*.
Inheriting from a function type thus makes sense if you want your class to behave like a simple function.
Inheriting from an extension function type however makes no sense and should be prohibited / frowned upon.
In a broad sense, providing type annotations on supertypes (which is what inheriting from an extension function is)
maybe should be diagnosed in the compiler (maybe not, more knowledge needed).

With this we'll get rid of classes `ExtensionFunction0`, `ExtensionFunction1`, ...
and the rest of this article will deal only with usual functions.

## Function0, Function1, ... types

The arity of the functional trait that the type checker can create in theory **is not limited** to any number,
but in practice should be limited to 255 on JVM.

These traits are named `kotlin.Function0<R>`, `kotlin.Function1<P0, R>`, ..., `kotlin.Function42<P0, P1, ..., P41, R>`, ...
They are *fictitious*, which means they have no sources and no runtime representation.
Type checker creates the corresponding descriptors on demand, IDE creates corresponding source files on demand as well.
Each of them inherits from `kotlin.Function` (described below) and contains a single
`fun invoke()` with the corresponding number of parameters and return type.

> TODO: investigate exactly what changes in IDE should be done and if they are possible at all.

On JVM function types are erased to the physical classes defined in package `kotlin.jvm.internal`:
`Function0`, `Function1`, ..., `Function22` and `FunctionN` for 23+ parameters.

## Function trait

There's also an empty trait `kotlin.Function<R>` for cases when e.g. you're storing functions with different/unknown arity
in a collection to invoke them reflectively somewhere else.

``` kotlin
package kotlin

trait Function<out R>
```

It's a physical trait, declared in platform-agnostic built-ins, and present in `kotlin-runtime.jar` for example.
However its declaration is **empty** and should be empty because every physical JVM function class `Function0`, `Function1`, ...
inherits from it (and adds `invoke()`), and we don't want to override anything besides `invoke()` when doing it from Java code.

## Functions with 0..22 parameters at runtime

There are 23 function traits in `kotlin.platform.jvm`: `Function0`, `Function1`, ..., `Function22`.
Here's `Function1` declaration, for example:

``` kotlin
package kotlin.platform.jvm

trait Function1<in P1, out R> : kotlin.Function<R> {
    fun invoke(p1: P1): R
}
```

These traits are supposed to be inherited from by Java classes when passing lambdas to Kotlin.

Package `kotlin.platform.jvm` is supposed to contain interfaces which help use Kotlin from Java.
(And not from Kotlin, because normally you would use a function type there,
most of the time even without mentioning built-in function classes: `(P1, P2, P3) -> R`.)

## Translation of Kotlin lambdas

There's also `FunctionImpl` abstract class at runtime which helps in implementing `arity` and vararg-invocation.
It inherits from all the physical function classes, unfortunately (more on that later).

``` kotlin
package kotlin.jvm.internal

abstract class FunctionImpl(override val arity: Int) :
    Function<Any?>,
    Function0<Any?>, Function1<Any?, Any?>, ..., ..., Function22<...>,
    FunctionN   // See the next section on FunctionN
{
    override fun invoke(): Any? {
        // Default implementations of all "invoke"s invoke "invokeVararg"
        // This is needed for KFunctionImpl (see below)
        assert(arity == 0)
        return invokeVararg()
    }
    
    override fun invoke(p1: Any?): Any? {
        assert(arity == 1)
        return invokeVararg(p1)
    }
    
    ...
    override fun invoke(p1: Any?, ..., p22: Any?) { ... }
    
    override fun invokeVararg(vararg p: Any?): Any? = throw UnsupportedOperationException()
    
    override fun toString() = ... // Some calculation involving generic runtime signatures
}
```

> TODO: sadly, this class needs to be implemented in Java because supertypes need to be **raw** classes
> for reflection to pick up correct generic signatures for inheritors

Each lambda is compiled to an anonymous class which inherits from `FunctionImpl` and implements the corresponding `invoke`:

``` kotlin
{ (s: String): Int -> s.length }

// is translated to

object : FunctionImpl(2), Function2<String, Int> {
    /* bridge */ fun invoke(p1: Any?): Any? = ...
    override fun invoke(p1: String): Int = p1.length
}
```

## Functions with more than 22 parameters at runtime

To support functions with many parameters there's a special trait in JVM runtime:

``` kotlin
package kotlin.platform.jvm

trait FunctionN<out R> : kotlin.Function<R> {
    val arity: Int
    fun invokeVararg(vararg p: Any?): R
}
```

> TODO: usual hierarchy problems: there are no such members in `kotlin.Function42` (it only has `invoke()`),
> so inheritance from `Function42` will need to be hacked somehow

And another type annotation:

``` kotlin
package kotlin.platform.jvm

annotation class arity(val value: Int)
```

A lambda type with 42 parameters on JVM is translated to `[arity(42)] FunctionN`.
A lambda is compiled to an anonymous class which overrides `invokeVararg()` instead of `invoke()`:

``` kotlin
object : FunctionImpl(42) {
    override fun invokeVararg(vararg p: Any?): Any? { ... /* code */ }
    // TODO: maybe assert that p's size is 42 in the beginning of invokeVararg?
}
```

> Note that `Function0`..`Function22` are provided primarily for **Java interoperability** and as an **optimization** for frequently used functions.
> We can change the number of functions easily from 23 to something else if we want to.
> For example, for `KFunction`, `KMemberFunction`, ... this number will be zero,
> since there's no point in implementing a hypothetical `KFunction5` from Java.

So when a large function is passed from Java to Kotlin, the object will need to inherit from `FunctionN`:

``` kotlin
    // Kotlin
    fun fooBar(f: Function42<*,*,...,*>) = f(...)
```

``` java
    // Java
    fooBar(new FunctionN<String>() {
        @Override
        public int getArity() { return 42; }
        
        @Override
        public String invokeVararg(Object... p) { return "42"; }
    }
```

> Note that `[arity(N)] FunctionN<R>` coming from Java code will be treated as `(Any?, Any?, ..., Any?) -> R`,
> where the number of parameters is `N`.
> If there's no `arity` annotation on the type `FunctionN<R>`, it won't be loaded as a function type,
> but rather as just a classifier type with an argument.


## Arity and invocation with vararg

There's an ability to get an arity of a function object and call it with variable number of arguments,
provided by extensions in **platform-agnostic** built-ins.

``` kotlin
package kotlin

intrinsic val Function<*>.arity: Int
intrinsic fun <R> Function<R>.invokeVararg(vararg p: Any?): R
```

But they don't have any implementation there.
The reason is, they need **platform-specific** function implementation to work efficiently.
This is the JVM implementation of the `arity` intrinsic (`invokeVararg` is essentially the same):

``` kotlin
fun Function<*>.calculateArity(): Int {
    return if (function is FunctionImpl) {  // This handles the case of lambdas created from Kotlin
        function.arity  // Note the smart cast
    }
    else when (function) {  // This handles all other lambdas, i.e. created from Java
        is Function0 -> 0
        is Function1 -> 1
        ...
        is Function22 -> 22
        is FunctionN -> function.arity  // Note the smart cast
        else -> throw UnsupportedOperationException()  // TODO: maybe do something funny here,
                                                       // e.g. find 'invoke' reflectively
    }
}
```

## `is`/`as` hack

The newly introduced `FunctionImpl` class inherits from all the `Function0`, `Function1`, ..., `FunctionN`.
This means that `anyLambda is Function2<*, *, *>` will be true for any Kotlin lambda.
To fix this, we need to hack `is` so that it would reach out to the `FunctionImpl` instance and get its arity.

``` kotlin
package kotlin.jvm.internal

// This is the intrinsic implementation
// Calls to this function are generated by codegen on 'is' against a function type
fun isFunctionWithArity(x: Any?, n: Int): Boolean = (x as? Function).arity == n
```

`as` should check if `isFunctionWithArity(instance, arity)`, and checkcast if it is or throw exception if not.

A downside is that `instanceof Function5` obviously won't work correctly from Java.

## How this will help reflection

The saddest part of this story is that all `K*Function*` interfaces should be hacked identically to functions.
The compiler should resolve `KFunctionN` for any `N`, IDEs should synthesize sources when needed,
`is`/`as` should be handled similarly etc.

However, we **won't introduce multitudes of `KFunction`s at runtime**.
The two reasons we did it for `Function`s were Java interop and lambda performance, and they both are not so relevant here.
A great aid was that the contents of each `Function` were trivial and easy to duplicate (23-plicate?),
which is not the case at all for `KFunction`s: they also contain code related to reflection.

So for reflection there will be:
* **fictitious** interfaces `KFunction0`, `KFunction1`, ..., `KMemberFunction0`, ..., `KMemberFunction42`, ... (defined in `kotlin`)
* **physical** interfaces `KFunction`, `KMemberFunction`, ... (defined in `kotlin.reflect`)
* **physical** JVM runtime implementation classes `KFunctionImpl`, `KMemberFunctionImpl`, ... (defined in `kotlin.reflect.jvm.internal`)

``` kotlin
package kotlin.reflect

trait KFunction<out R> : Function<R> {
    fun invokeVararg(vararg p: Any?): R
    ... // Reflection-specific declarations
}
```

``` kotlin
package kotlin.reflect.jvm.internal

open class KFunctionImpl(name, owner, arity, ...) : KFunction<Any?>, FunctionImpl(arity) {
    ... // Reflection-specific code
    
    // Remember that each "invoke" delegates to "invokeVararg" with assertion by default.
    // We're overriding only "invokeVararg" here and magically a callable reference
    // will start to work as the needed Function* class
    override fun invokeVararg(vararg p: Any?): Any? {
        owner.getMethod(name, ...).invoke(p)  // Java reflection
    }
}
```

> TODO: a performance problem: we pass arity to `FunctionImpl`'s constructor,
> which may involve a lot of the eager computation (finding the method in a Class).
> Maybe make `arity` an abstract property in `FunctionImpl`, create a subclass `Lambda` with a concrete field for lambdas,
> and for `KFunction`s just implement it lazily


