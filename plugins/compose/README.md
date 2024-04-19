# Compose Readme

Jetpack Compose makes it easy to write and manage an application's frontend by providing a declarative API that allows users to update their Android application UI without imperatively mutating frontend views.

A Compose application is comprised of `@Composable` functions, which are functions that transform application data into a UI hierarchy.  When the underlying data changes, the Composable functions can be re-invoked to generate an updated UI hierarchy.

```
import androidx.compose.material.*
import androidx.compose.runtime.*

@Composable
fun Greeting(name: String) {
   Text(text = "Hello $name!")
}
```

Compose functions should be side-effect free, and should only read data that was explicitly passed into the composable function from the caller (eg. do not read from globals).

The compose compiler plugin makes use of some experimental extension points to the Kotlin compiler.  In particular, an extension point that allows us to intercept the invocation of composable functions.  You may also see some references to an XML-like syntax (known internally as KTX) which is our old syntax from before the method interception was a thing.  We are transitioning from the KTX syntax to using a Kotlin DSL based on intercepted function calls.

## Feedback
To provide feedback or report bugs, please refer to the main AndroidX contribution guide and report your bugs [here](https://issuetracker.google.com/issues/new?component=610764)


