# Multi-declarations in Parameters

* **Timeframe**: M10
* **Why**: Breaking changes in syntactic structures (lambdas)

## Goal

Support multi-declarations in parameters of lambdas, functions, constructors and setters.

## Examples

``` kotlin
// decomposing pairs in a lambda
listOfPairs.map {
  (a, b) -> a + b
}

// decompose a parameter:
fun foo((a, b): Pair<Int, String>, c: Bar)
// can be called as
foo(pair, bar)

// decompose a constructor parameter
class C(val (a, b): Pair<Int, String>) {}
```

## TODO

  - nested decompositions?
  - grammar
  - required return type for lambdas with `return`

## Syntax

Old lambda syntax:

``` kotlin
{ a, b -> ... } // two parameters
{ (a, b) -> ... } // two parameters
{ (a: Int, b: String) -> ... } // parameter types
{ (a, b): Int -> ... } // return type
{ T.(a, b): Int -> ... } // receiver type
```

New syntax:

Common short form:
``` kotlin
{ a -> ... } // one parameter
{ a, b -> ... } // two parameters
{ (a, b) -> ... } // a decomposed pair
{ (a, b), c -> ... } // a decomposed pair and another parameter
{ ((a, b), c) -> ... } // ??? a decomposed pair whose first component is a pair
```

No return type nor receiver type in the short form:
``` kotlin
{ a: A -> ... } // one parameter
{ a, b: B -> ... } // two parameters
{ (a, b: B) -> ... } // a decomposed pair
{ (a, b): Pair<A, B> -> ... } // a decomposed pair
{ (a, b: B), c -> ... } // a decomposed pair and another parameter
{ ((a, b), c: C) -> ... } // ??? a decomposed pair whose first component is a pair
```

> (BAD DECISION) To disambiguate, we could demand a prefix:
``` kotlin
{ fun Recv.(((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun ( ((a, b), c) ): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a, b), c: C)) -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a, b), c): Pair<Pair<A, B>, C>) -> ... } // ??? a decomposed pair whose first component is a pair
```
Rather hairy.

But we have this form possibly coming (needed for local returns):
``` kotlin
foo(fun(): R {
    return r // local return
})
```

We can't omit return type in this form. But we use it only when we need return type/receiver type:
``` kotlin
fun Recv.(((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R { ... } // a decomposed pair whose first component is a pair
fun (((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R { ... } // a decomposed pair whose first component is a pair
fun ( ((a, b), c) ): R { ... } // ??? a decomposed pair whose first component is a pair
fun (((a, b), c: C)): R { ... } // ??? a decomposed pair whose first component is a pair
fun (((a, b), c): Pair<Pair<A, B>, C>): R { ... } // ??? a decomposed pair whose first component is a pair
fun (a) {} // return type is Unit
```
Difference from normal functions: we can omit parameter types, we can omit the name (don't have to).
Difference from lambdas: can specify return type and receiver type + returns are local.

### Quick summary of syntactic changes

- functions
  - allow multi-declarations in parameters
  - allow functions as expressions (anonymous or named)
- lambdas
  - allow multi-declarations in lambda parameters
  - no return type/receiver type in lambda parameters, use anonymous function instead

### Grammar

TODO

## PSI changes

Create a common superclass for lambdas and anonymous functions, most clients shouldn't notice the change

## Front-end checks and language rules

New concept introduced: "function expression" (looks like a function declaration, but works as an expression) as opposed to "lambda" (both are special cases of "function literal").

### Function call sites

From the outside a multi-declared parameter is seen as one parameter of the specified type:
``` kotlin
foo(pair) // caller can not pass two separate values here
```

No changes to the call-site checking are required.

### Function declaration Sites

Function *declarations* are not allowed to omit types of their parameters:
``` kotlin
fun foo((a, b): Pair<A, B>) {...} // type is required
```

Types of individual components of the multi-declarations are optional:
``` kotlin
fun foo((a: A, b: B): Pair<A, B>) {...} // individual types of `a` and `b` are not required
```

Default values are only allowed for whole parameters, not for individual components:
``` kotlin
fun foo((a, b): AB = AB(1, 2)) {...}
```

All names in the parameter list belong to one and the same namespace:
``` kotlin
fun foo((a, b): AB, a: A) // redeclaration: two variables named `a`
```

One can use components of previously declared parameters in default values:
``` kotlin
fun foo((a, b): AB, c: C = C(a, b)) {...}
```

## Semantics and Back-end changes

TODO
- what is the Java name of this parameter: `(a, b): Pair<A, B>`?
- make components available in decault parameter values

## IDE Changes

New intentions:
- Convert lambda <-> anonymous function (mind the returns!)

Affected functionality:
- Change signature
- Move lambda outside/inside parentheses
- Specify types explicitly in a lambda (use conversion to anonymous function)

## Open question: Nested decompositions

``` kotlin
fun foo((a, (b, c)): Pair<Int, Pair<String, Any>>)
```
