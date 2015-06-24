# Exhaustive When Statements over Enums

Goal: keep control under possible extension of enum. If a new enum entry appears, it should be possible to have a diagnostic
on a non-exhaustive when over the extended enum.

## Discussion

There are several possible usages of when over enum. Let `OurEnum` be

``` kotlin
enum class OurEnum {
    FIRST,
    SECOND,
    UNKNOWN,
    LAST
}
```

### Expressions

``` kotlin
fun encode(oe: OurEnum): Int = when(oe) {
    OurEnum.FIRST   -> 1
    OurEnum.SECOND  -> 2
    OurEnum.UNKNOWN -> 100
    OurEnum.LAST    -> 1000
}
```

It should be exhaustive by default, no extra efforts required. If it's non-exhaustive, we get just NO_ELSE_IN_WHEN.
Warning about non-exhaustive when here is not important but possible.

### Initialization / Return Statements

``` kotlin
val v: Int
when (oe) {
    OurEnum.FIRST   -> v = 1    // or return 1
    OurEnum.SECOND  -> v = 2    // or return 2
    OurEnum.UNKNOWN -> v = 100  // or return 100
    OurEnum.LAST    -> v = 1000 // or return 1000
}
return v // or nothing
```

Such whens also have to be exhaustive, otherwise we get UNINITIALIZED_VARIABLE or NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.
Warning about non-exhaustive when here is not important but possible.

Ideally, such whens should be replaced by expression whens.

### Generally Exhaustive Statements

``` kotlin
when(oe) {
    OurEnum.FIRST, OurEnum.SECOND -> proceedFirstOrSecond(oe)
    OurEnum.UNKNOWN -> proceedUnknown()
    OurEnum.LAST    -> proceedLast()
}
```

Here we want to do something for any enum entry. 
If enum is extended with an entry like `THIRD`, we would like to get some warning (or even error) about non-exhaustive when.
 
### Generally Non-Exhaustive Statements

``` kotlin
when(oe) {
    OurEnum.FIRST -> checkFirst()
    OurEnum.LAST -> checkLast()
}
```

Here we want to do something only for some fixed entry set, e.g. FIRST and LAST only.
We do not want to get any warnings or errors.

## Possible Solutions

### Do Nothing

We just ignore "generally exhaustive statements", considering this use-case as not important.

### Always Check Exhaustiveness in Compiler

We check all when over enums for exhaustiveness, with a possible exception for expression whens.
Compiler reports a warning about it. 
We just ignore "generally non-exhaustive statements", considering this use-case as not important.

It's a current implementation according with KT-6227.

### Always Check Exhaustiveness in IDE

We do the same but as IDE inspection, and provide a weak warning for non-exhaustive whens.
Same as above but a little bit softer.

### Exhaustive When Annotation

In this case we just write

``` kotlin
exhaustive when(oe) {
    OurEnum.FIRST, OurEnum.SECOND -> proceedFirstOrSecond(oe)
    OurEnum.UNKNOWN -> proceedUnknown()
    OurEnum.LAST    -> proceedLast()
}
```

if we need when exhaustiveness. 

Compiler checks whens with this annotation and provides an error (not even a warning) if it's not consistent with the following code.
At this moment, it looks like this variant is most satisfiable.

### Non-Exhaustive (Partial?) When Annotation

In this case we just write

``` kotlin
partial when(oe) {
    OurEnum.FIRST -> checkFirst()
    OurEnum.LAST -> checkLast()
}
```

if we do not need when exhaustiveness. 

Compiler checks when without this annotation and provides a warning if such a when is not exhaustive.
Otherwise compiler ignores exhaustiveness.

