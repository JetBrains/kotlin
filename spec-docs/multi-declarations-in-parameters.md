# Multi-declarations in Parameters

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

  - nested decompositions

## Syntax

### Lambdas

Old syntax:

```
{ a, b -> ... } // two parameters
{ (a, b) -> ... } // two parameters
{ (a: Int, b: String) -> ... } // parameter types
{ (a, b): Int -> ... } // return type
{ T.(a, b): Int -> ... } // receiver type
```

New syntax:

```
Common short form:
{ a -> ... } // one parameter
{ a, b -> ... } // two parameters
{ (a, b) -> ... } // a decomposed pair
{ (a, b), c -> ... } // a decomposed pair and another parameter
{ ((a, b), c) -> ... } // ??? a decomposed pair whose first component is a pair

No return type nor receiver type in the short form:
{ a: A -> ... } // one parameter
{ a, b: B -> ... } // two parameters
{ (a, b: B) -> ... } // a decomposed pair
{ (a, b): Pair<A, B> -> ... } // a decomposed pair
{ (a, b: B), c -> ... } // a decomposed pair and another parameter
{ ((a, b), c: C) -> ... } // ??? a decomposed pair whose first component is a pair

(BAD DECISION) To disambiguate, we could demand a prefix:
{ fun Recv.(((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun ( ((a, b), c) ): R -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a, b), c: C)) -> ... } // ??? a decomposed pair whose first component is a pair
{ fun (((a, b), c): Pair<Pair<A, B>, C>) -> ... } // ??? a decomposed pair whose first component is a pair
Rather hairy.

But we have this form possibly coming (needed for local returns):
foo(fun(): R {
    return r // local return
})
We can't omit return type in this form. But we use it only when we need return type/receiver type:

fun Recv.(((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R { ... } // a decomposed pair whose first component is a pair
fun (((a: A, b: B): Pair<A, B>, c: C): Pair<Pair<A, B>, C>): R { ... } // a decomposed pair whose first component is a pair
fun ( ((a, b), c) ): R { ... } // ??? a decomposed pair whose first component is a pair
fun (((a, b), c: C)): R { ... } // ??? a decomposed pair whose first component is a pair
fun (((a, b), c): Pair<Pair<A, B>, C>): R { ... } // ??? a decomposed pair whose first component is a pair
fun (a) {} // return type is Unit
Difference from normal functions: we can omit parameter types, we can omit the name (don't have to).
Difference from lambdas: can specify return type and receiver type + returns are local.
```


```
multi-parameter:
  "("
```

## Open question: Nested decompositions

``` kotlin
fun foo((a, (b, c)): Pair<Int, Pair<String, Any>>)
```





