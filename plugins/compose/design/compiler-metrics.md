# Interpreting Compose Compiler Metrics

## Context

The Compose Compiler plugin can generate reports / metrics around certain compose-specific concepts
that can be useful to understand what is happening with some of your compose code at a fine-grained
level.

## Enabling Metrics

### AndroidX repository

In the AndroidX repository `./gradlew` accepts options to enable reports.

To enable compiler metrics for a build target include `-Pandroidx.enableComposeCompilerMetrics=true`
prior to the build target such as:

```
.gradlew -Pandroidx.enableComposeCompilerMetrics=true :compose:runtime:runtime:compileKotlin
```

To enable compiler reports for a build target include `-Pandroidx.enableComposeCompilerReports=true`
prior to the build target such as:

```
.gradlew -Pandroidx.enableComposeCompilerReports=true :compose:runtime:runtime:compileKotlin
```

### Other Gradle projects

To enable metrics for a gradle module, include:

```
compileKotlin {
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=<directory>"
    )
}
```

where `<directory>` is replaced with the location you wish the report written.

To enabled reports for a gradle module, include:

```
compileKotlin {
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=<directory>"
    )
}
```

where `<directory>` is replaced with the location you wish the report written.

## Reports Breakdown

### Top Level Metrics (<your-module>-module.json)

This report shows some high level metrics that are compose specific. This is mostly to create
numeric data points which can be tracked over time. The ratios between some of these numbers can be
interesting: for instance the number of “certainArguments” versus “totalArguments” can give you a
percentage of arguments to composable calls that have metadata propagated.

Here’s an example of the report for the foundation module:

```
{
  "skippableComposables": 53,
  "restartableComposables": 60,
  "readonlyComposables": 1,
  "totalComposables": 100,
  "restartGroups": 60,
  "totalGroups": 139,
  "staticArguments": 25,
  "certainArguments": 138,
  "knownStableArguments": 377,
  "knownUnstableArguments": 25,
  "unknownStableArguments": 24,
  "totalArguments": 426,
  "markedStableClasses": 8,
  "inferredStableClasses": 28,
  "inferredUnstableClasses": 0,
  "inferredUncertainClasses": 0,
  "effectivelyStableClasses": 36,
  "totalClasses": 36,
  "memoizedLambdas": 40,
  "singletonLambdas": 6,
  "singletonComposableLambdas": 4,
  "composableLambdas": 49,
  "totalLambdas": 81
}
```

### Composable Signatures (<your module>-composables.txt)

This report is intended to be consumed by humans, and is printed in pseudo-kotlin style function
signatures. This report shows every composable function in the module, and breaks down each
parameter and information about each. This report indicates if the overall composable is
restartable, skippable, or readonly. Each parameter is marked to be either stable or unstable. And
each default parameter expression is marked as either static or dynamic.

```
restartable fun Image(
  unstable bitmap: ImageBitmap
  stable contentDescription: String?
  stable modifier: Modifier? = @static Companion
  stable alignment: Alignment? = @dynamic Companion.Center
  stable contentScale: ContentScale? = @dynamic Companion.Fit
  stable alpha: Float = @static DefaultAlpha
  stable colorFilter: ColorFilter? = @static null
)
```

### Composables Table (<your module>-composables.csv)

This report is a CSV and is intended to be easily thrown into a spreadsheet and digested that way.
This holds high level metrics specific to every composable function.

### Classes (<your module>-classes.txt)

This report is also meant to be consumed by a human. It is written in pseudo-kotlin style class
signatures. This file is primarily meant for you to understand how the stable inferencing algorithm
interpreted a given class. Each class is indicated at the top level as being either stable,
unstable, or runtime. Runtime means that stability depends on other dependencies which will be
resolved at runtime (a type parameter or a type in an external module). Stability is determined by
the fields on the class, so each field is displayed as part of the class, and each field is marked
as either stable, unstable, or runtime stable as well. The <runtime stability> line at the bottom
indicates the “expression” that is used to resolve this stability at runtime.

```
stable class CornerBasedShape {
  stable val topStart: CornerSize
  stable val topEnd: CornerSize
  stable val bottomEnd: CornerSize
  stable val bottomStart: CornerSize
  <runtime stability> = Stable
}
```

## Things To Look Out For

### Functions that are `restartable` but not `skippable`

In a composables.txt file, you may see some composable functions which are marked as restartable
but not marked as skippable. These two concepts are closely related, but distinct.

Skippability means that when called during recomposition, compose is able to skip the function if
all of the parameters are equal. Skippability is often very important for public APIs, and can have
a big performance impact if the chances of a composable getting called with the same inputs is
high. The typical reason for a function to not be skippable is when one or more of its parameters
types are not considered `Stable`.

Restartability means that this function serves as a “scope” where recomposition can start. Any
function that is skippable must be restartable for correctness, but functions can be restartable
and not skippable. Though restartability is needed for correctness when a function is skippable, it
can sometimes be beneficial even if the function is not skippable. If the function reads a `State`
value during its execution that is very likely to change (for instance, an animated value), then
restartability is very important, as if it is not restartable, then compose will use an ancestor
scope to initiate the recomposition when that state value changes.

If you see a function that is restartable but not skippable, it’s not always a bad sign, but it
sometimes is an opportunity to do one of two things:

  1. Make the function skippable by ensuring all of its parameters are stable
  2. Make the function not restartable by marking it as a `@NonRestartableComposable`

It is a good idea to do (1) if the function is a highly used public API, and if you think the
parameters not being stable is an oversight.

It is a good idea to do (2) if the composable function is unlikely to ever be the “root” of a
recomposition. In other words, if the composable function doesn’t directly read any state variables,
it is unlikely that this restart scope is ever being used. This can be very difficult for the
compiler to determine though, so the restart scope is generated anyway unless you specify otherwise
directly with a `@NonRestartableComposable` annotation.

### Default parameter expressions that are `@dynamic`

Composable functions make heavy use of parameters default expressions. This is an important tool
that allows a composable to have an API that is both configurable and easy to use. Default
expressions are capable of executing any code that can be executed in the body of the function, so
for composable functions, that includes making calls to other composables. It also means that a
default expression can read a state variable and automatically cause the composable to get
subscribed to the state.

In order for default parameters to have all of this power, the compose compiler often has to
generate a fair amount of code around default expressions to make sure they behave in a predictable
and correct manner. This is only really necessary for composable calls, and for state reads, but it
is really difficult for the compiler to guarantee that an expression will not perform a state read,
since state reads can happen almost anywhere.

In the `composables.txt` file, you will see all default parameter value expressions prefixed with
either `@static` or `@dynamic`. You may find that the compiler is treating it as `@dynamic` even
though it seems like it should be `@static`. If this is the case, you should strive to make this
expression `@static` by making it an expression that compose can infer as such. You can do this by
marking the value as `@Stable`.

Default expressions should be `@static` in every case except for the following two cases:

  1. You are explicitly reading an observable dynamic variable. Composition Locals and state
     variables are an important example of this. In these cases, you need to rely on the fact that
     the default expression will be re-executed when the value changes.

  2. You are explicitly calling a composable function, such as `remember`. The most common use case
     for this is state hoisting.

### Classes that are unstable

In the `classes.txt` file, you may see classes that are unstable. Not all classes need to be stable,
but a class being stable unlocks a lot of flexibility for the compose compiler to make
optimizations when a stable type is being used in places, which is why it is such an important
concept for compose.

The compose compiler will infer whether or not a given class is stable or not at compile time, and
its algorithm for doing that is not all that complicated. The compiler will look at all of the
fields on the class, and it cannot be inferred as stable if any one of the fields is:

  1. The field is mutable (it is associated with a `var` property)
  2. The field has a non-stable type

If any one of the fields on the class meets this criteria, it will be inferred as `unstable`, but
that doesn’t mean that it can’t be marked as stable. Sometimes mutable fields are used in ways that
are still safe in the context of Stability guarantees. For instance, a very common case is to use a
field to “cache” the result of some calculation. If the caching is only done for performance
reasons, and the public API of that class makes it impossible to know whether the value is “cached”
or not, then the class could still be marked stable.

In order to find the field(s) which are causing a class to be unstable, you simply need to look for
any field in the class that is a `var` or an `unstable val`.
