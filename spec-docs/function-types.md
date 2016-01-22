# Function Types in Kotlin on JVM

## Goals

* Get rid of 23 hardwired physical function classes. One of the problems with them is that they should be effectively duplicated in reflection which means a lot of physical classes in kotlin-runtime.jar.
* Make extension functions assignable to normal functions (and vice versa), so that it's possible to do `listOfStrings.map(String::length)`
* Allow functions with more than 23 parameters, theoretically any number of parameters (in practice 255 on JVM).
* At the same time, allow to implement Kotlin functions easily from Java: `new Function2() { ... }` and overriding `invoke` only would be the best.
Enabling SAM conversions on Java 8 would also be terrific.

## Brief solution overview

* Treat extension functions almost like non-extension functions with one extra parameter, allowing to use them almost interchangeably.
* Introduce a physical class `Function` and unlimited number of *fictitious* (synthetic) classes `Function0`, `Function1`, ... in the compiler front-end
* On JVM, introduce `Function0`..`Function22`, which are optimized in a certain way,
and `FunctionN` for functions with 23+ parameters.
When passing a lambda to Kotlin from Java, one will need to implement one of these interfaces.
* Also on JVM (under the hood) add abstract `FunctionImpl` which implements all of `Function0`..`Function22` and `FunctionN`
(throwing exceptions), and which knows its arity.
Kotlin lambdas are translated to subclasses of this abstract class, passing the correct arity to the super constructor.
* Provide a way to get arity of an arbitrary `Function` object (pretty straightforward).
* Hack `is/as Function5` on any numbered function in codegen (and probably `KClass.cast()` in reflection) to check against `Function` and its arity.

## Extension functions

Extension function type `T.(P) -> R` is now just a shorthand for `@ExtensionFunctionType Function2<T, P, R>`.
`kotlin.extension` is a **type annotation** defined in built-ins.
So effectively functions and extension functions now have the same type,
which means that everything which takes a function will work with an extension function and vice versa.

To prevent unpleasant ambiguities, we introduce additional restrictions:
* A value of an extension function type cannot be **called** as a function, and a value of a non-extension
function type cannot be called as an extension. This requires an additional diagnostic which is only fired
when a call is resolved to the `invoke` with the wrong extension-ness.
(Note that this restriction is likely to be lifted, so that extension functions can be called as functions,
but not the other way around.)
* Shape of a function **literal** argument or a function expression must exactly match
the extension-ness of the corresponding parameter. You can't pass an extension function **literal**
or an extension function expression where a function is expected and vice versa.
If you really want to do that, change the shape, assign literal to a variable or use the `as` operator.

So basically you can now safely coerce values between function and extension function types,
but still should invoke them in the format which you specified in their type (with or without `@ExtensionFunctionType`).

With this we'll get rid of classes `ExtensionFunction0`, `ExtensionFunction1`, ...
and the rest of this article will deal only with usual functions.

## Function0, Function1, ... types

The arity of the functional interface that the type checker can create in theory **is not limited** to any number,
but in practice should be limited to 255 on JVM.

These interfaces are named `kotlin.Function0<R>`, `kotlin.Function1<P0, R>`, ..., `kotlin.Function42<P0, P1, ..., P41, R>`, ...
They are *fictitious*, which means they have no sources and no runtime representation.
Type checker creates the corresponding descriptors on demand, IDE creates corresponding source files on demand as well.
Each of them inherits from `kotlin.Function` (described below) and contains only two functions,
both of which should be synthetically produced by the compiler:
* (declaration) `invoke` with no receiver, with the corresponding number of parameters and return type.
* (synthesized) `invoke` with first type parameter as the extension receiver type, and the rest as parameters and return type.

Call resolution should use the annotations on the type of the value the call is performed on
to select the correct `invoke` and to report the diagnostic if the `invoke` is illegal (see the previous block).

On JVM function types are erased to the physical classes defined in package `kotlin.jvm.internal`:
`Function0`, `Function1`, ..., `Function22` and `FunctionN` for 23+ parameters.

## Function interface

There's also an empty interface `kotlin.Function<R>` which is a supertype for all functions.

``` kotlin
package kotlin

interface Function<out R>
```

It's a physical interface, declared in platform-agnostic built-ins, and present in `kotlin-runtime.jar` for example.
However its declaration is **empty** and should be empty because every physical JVM function class `Function0`, `Function1`, ...
inherits from it (and adds `invoke()`), and we don't want to override anything besides `invoke()` when doing it from Java code.

## Functions with 0..22 parameters at runtime

There are 23 function interfaces in `kotlin.jvm.functions`: `Function0`, `Function1`, ..., `Function22`.
Here's `Function1` declaration, for example:

``` kotlin
package kotlin.jvm.functions

interface Function1<in P1, out R> : kotlin.Function<R> {
    fun invoke(p1: P1): R
}
```

These interfaces are supposed to be inherited from by Java classes when passing lambdas to Kotlin.
They shouldn't be used from Kotlin however, because normally you would use a function type there,
most of the time even without mentioning built-in function classes: `(P1, P2, P3) -> R`.

## Translation of Kotlin lambdas

There's also `FunctionImpl` abstract class at runtime which helps in implementing `arity` and vararg-invocation.
It inherits from all the physical function classes, unfortunately (more on that later).

``` java
package kotlin.jvm.internal;

// This class is implemented in Java because supertypes need to be raw classes
// for reflection to pick up correct generic signatures for inheritors
public abstract class FunctionImpl implements
    Function0, Function1, ..., ..., Function22,
    FunctionN   // See the next section on FunctionN
{
    public abstract int getArity();
    
    @Override
    public Object invoke() {
        // Default implementations of all "invoke"s invoke "invokeVararg"
        // This is needed for KFunctionImpl (see below)
        assert getArity() == 0;
        return invokeVararg();
    }
    
    @Override
    public Object invoke(Object p1) {
        assert getArity() == 1;
        return invokeVararg(p1);
    }
    
    ...
    @Override
    public Object invoke(Object p1, ..., Object p22) { ... }

    @Override    
    public Object invokeVararg(Object... args) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String toString() {
        // Some calculation involving generic runtime signatures
        ...
    }
}
```

Each lambda is compiled to an anonymous class which inherits from `FunctionImpl` and implements the corresponding `invoke`:

``` kotlin
{ (s: String): Int -> s.length }

// is translated to

object : FunctionImpl(), Function1<String, Int> {
    override fun getArity(): Int = 1

    /* bridge */ fun invoke(p1: Any?): Any? = ...
    override fun invoke(p1: String): Int = p1.length
}
```

## Functions with more than 22 parameters at runtime

To support functions with many parameters there's a special interface in JVM runtime:

``` kotlin
package kotlin.jvm.functions

interface FunctionN<out R> : kotlin.Function<R> {
    val arity: Int
    fun invokeVararg(vararg p: Any?): R
}
```

> TODO: usual hierarchy problems: there are no such members in `kotlin.Function42` (it only has `invoke()`),
> so inheritance from `Function42` will need to be hacked somehow

And another type annotation:

``` kotlin
package kotlin.jvm.functions

annotation class arity(val value: Int)
```

A lambda type with 42 parameters on JVM is translated to `@arity(42) FunctionN`.
A lambda is compiled to an anonymous class which overrides `invokeVararg()` instead of `invoke()`:

``` kotlin
object : FunctionImpl() {
    override fun getArity(): Int = 42

    override fun invokeVararg(vararg p: Any?): Any? { ... /* code */ }
    // TODO: maybe assert that p's size is 42 in the beginning of invokeVararg?
}
```

> Note that `Function0`..`Function22` are provided primarily for **Java interoperability** and as an **optimization** for frequently used functions.
> We can change the number of functions easily from 23 to something else if we want to.
> For example, for `KFunction` this number will be zero,
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

> Note that `@arity(N) FunctionN<R>` coming from Java code will be treated as `(Any?, Any?, ..., Any?) -> R`,
> where the number of parameters is `N`.
> If there's no `@arity` annotation on the type `FunctionN<R>`, it won't be loaded as a function type,
> but rather as just a classifier type with an argument.


## Arity and invocation with vararg

There's an ability to get an arity of a function object and call it with variable number of arguments,
provided by extensions in **platform-agnostic** built-ins.

``` kotlin
package kotlin

@intrinsic val Function<*>.arity: Int
@intrinsic fun <R> Function<R>.invokeVararg(vararg p: Any?): R
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

## `is`/`as`

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

A downside is that `instanceof Function5` obviously won't work correctly from Java. We should provide a public facade to `isFunctionWithArity` which should be used from Java instead of `instanceof`.

Also we should issue warnings on `is Array<Function2<*, *, *>>` (or `as`), since it won't work for empty arrays (there's no instance of `FunctionImpl` to reach out and ask the arity).

## How this will help reflection

`KFunction*` interfaces should be synthesized at compile-time identically to functions.
The compiler should resolve `KFunction{N}` for any `N`, IDEs should synthesize sources when needed,
`is`/`as` should be handled similarly etc.

However, we **won't introduce multitudes of `KFunction`s at runtime**.
The two reasons we did it for `Function`s were Java interop and lambda performance, and they both are not so relevant here.
A great aid was that the contents of each `Function` were trivial and easy to duplicate (23-plicate?),
which is not the case at all for `KFunction`s: they also contain code related to reflection.

So for reflection there will be:
* **fictitious** interfaces `KFunction0`, `KFunction1`, ..., `KFunction42`, ... (defined in `kotlin.reflect`)
* **physical** interface `KFunction` (defined in `kotlin.reflect`)
* **physical** JVM runtime implementation class `KFunctionImpl` (defined in `kotlin.reflect.jvm.internal`)

As an example, `KFunction1` is a fictitious interface (in much the same manner that `Function1` is)
which inherits from `Function1` and `KFunction`. The former lets you call a type-safe `invoke` on a
callable reference, and the latter allows you to use reflection features on the callable reference.

``` kotlin
fun foo(s: String) {}

fun test() {
    ::foo.invoke("")  // ok, calls Function1.invoke
    ::foo.name        // ok, calls KFunction.name
}
```
