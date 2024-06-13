# Strong Skipping Mode

Strong Skipping is an experimental mode added to the Compose compiler in version 1.5.4.
With strong skipping enabled, the compiler changes its behavior in two ways:
- Composables with unstable parameters are now skippable
- Lambdas with unstable captures will be memoized

>**Warning**: We do not currently consider this mode production ready. We have enabled it in the Compose 1.7 alpha and will evaluate
it before reaching beta.
## Composable skippability
Strong skipping mode relaxes some of the stability rules normally applied by the Compose compiler
when it comes to skipping and composable functions. By default, the Compose compiler will mark a composable function
as skippable if it only has stable values provided as arguments. Strong skipping mode changes this.

With strong skipping enabled, **all restartable composable functions will be skippable**, regardless
of if they have unstable parameters or not. Non-restartable composable functions remain unskippable.

To determine whether to skip a composable during recomposition, unstable parameters are compared with their previous
values using instance equality. Stable parameters continue to be compared with their previous values using object equality - `Object.equals()`.

If all parameters meet these requirements, the composable is skipped during recomposition.

If you want to opt out a composable function from strong skipping, i.e. you want a restartable but non-skippable composable, you can use the
`@NonSkippableComposable` annotation.

```
@NonSkippableComposable
@Composable
fun MyNonSkippableComposable {}
```

### Do I still need to annotate classes with `@Stable`?
You will still need to annotate a class with `@Stable` if you want the object compared with object equality instead of instance equality.

## Lambda memoization
Strong skipping mode also enables more aggressive memoization of lambdas inside composable functions. By default, the Compose compiler
will memoize lambdas in composable functions that only capture stable values, additionally composable lambdas are always memoized.

>Note: Lambdas with no captures are also memoized, however
this is done by the Kotlin compiler and not by the Compose compiler plugin.

With strong skipping enabled, **lambdas with unstable captures are also memoized**.

Effectively this is wrapping your lambda with a `remember` call, keyed with the captures of the lambda, automatically e.g.
```
@Composable
fun MyComposable(unstableObject: Unstable, stableObject: Stable) {
    val lambda = {
        use(unstableObject)
        use(stableObject)
    }
}
```
roughly becomes the following with strong skipping enabled
```
@Composable
fun MyComposable(unstableObject: Unstable, stableObject: Stable) {
    val lambda = remember(unstableObject, stableObject) {
        {
            use(unstableObject)
            use(stableObject)
        }
    }
}
```
The keys follow the same comparison rules as composable functions, unstable keys are compared using instance equality and stable keys are compared using object equality.

>Note: This is slightly different to a normal `remember` call where all keys are compared using object equality.

Doing this optimization greatly increases the number of composables that will skip during recomposition as without this memoization,
any composable that takes a lambda parameter will most likely have a new lambda allocated during recomposition and therefore will not have equal
parameters to the last composition.

> Note: A common misconception is that lambdas with unstable captures are themselves unstable objects. This is not true, lambdas are always
considered stable, however as they were not memoized and reallocated during recomposition, they lead to composables that were not skipped due to unequal parameters.

If you have a lambda that you do not want memoized, you can use the `@DontMemoize` annotation.

```
val lambda = @DontMemoize {
    ...
}
```

## Enabling strong skipping mode
### AndroidX repository

We have enabled this option in AndroidX for all Compose modules.

### Other Gradle projects

To strong skipping for a gradle module, include:

```
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    compilerOptions.freeCompilerArgs.addAll(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=StrongSkipping",
    )
}
```