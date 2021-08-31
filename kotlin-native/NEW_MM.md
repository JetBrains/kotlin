# New memory model migration guide

**NOTE**: _The new MM is still in an experimental stage. It's **not** production-ready._

In the new MM we are lifting restrictions placed on object sharing: there's no need to freeze objects to share them
between threads.

In particular:
* Top level properties can be accessed and modified by any thread without the need to use `@SharedImmutable`.
* Objects passing through interop can be accessed and modified by any thread without the need to freeze them.
* `Worker.executeAfter` will no longer require `operation` to be frozen, and `Worker.execute` will no longer require
  `producer` to return an isolated object subgraph.

A few caveats:
* As with the previous MM, memory is not reclaimed eagerly: an object is reclaimed only when GC happens. This extends
  to Swift/ObjC objects that crossed interop boundary into Kotlin/Native.
* `AtomicReference` from `kotlin.native.concurrent` still requires freezing the `value`. `FreezableAtomicReference`
  can be used instead, or, alternatively, `AtomicRef` from `atomicfu` can be used (**NOTE**: _`atomicfu` has not reached 1.x yet_).
* `deinit` on Swift/ObjC objects (and the objects referred by them) will be called on a different thread if these objects
  cross interop boundary into Kotlin/Native.
* When calling Kotlin suspend functions from Swift, completion handlers might be called on threads other than the main.

Together with the new MM we are bringing in another set of changes:
* Global properties are initialized lazily, when the file they are defined in is first accessed. Previously global properties were
  initialized at the program startup.
  This is in line with Kotlin/JVM. As a workaround, properties that must be initialized at the program start can be marked with `@EagerInitialization`
  (please, consult the docs for `@EagerInitialization` before using).
* `by lazy {}` properties support thread safety modes and do not handle unbounded recursion. This is in line with Kotlin/JVM.
* Exceptions escaping `operation` in `Worker.executeAfter` are processed like in other parts of the runtime:
  by trying to execute a user-defined unhandled exception hook, or terminating the program if the hook was not found or
  failed with exception itself.

## Enable the new MM

**NOTE**: _The new MM is still in an experimental stage. It's **not** production-ready._

### Update the Kotlin/Native compiler

Update to Kotlin/Native 1.6.0-M1-139 and enable dev repositories:
```kotlin
// build.gradle.kts

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

// settings.gradle.kts

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        gradlePluginPortal()
    }
}
```

### Switch to the new memory model

Add compilation flag `-Xbinary=memoryModel=experimental`. With `gradle` it's enough to append this line to `gradle.properties`:
```properties
kotlin.native.binary.memoryModel=experimental
```

Alternatively,
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        binaryOptions["memoryModel"] = "experimental"
    }
}
```

### Update the libraries

To fully take advantage of the new MM, newer versions of libraries were released:
* `kotlinx.coroutines`: `1.5.1-new-mm-dev2` at https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven
  * No freezing, every common primitive (Channels, Flows, coroutines) work through worker boundaries.
  * Unlike `native-mt` version, library objects are transparent for `freeze`: if you freeze e.g. a channel, all of its internals will get frozen, 
    and it won't work as expected. In particular, this can happen when freezing something that captures a channel.
  * `Dispatchers.Default` is backed by a pool of workers on Linux and Windows, and by a global queue on Apple targets.
  * `newSingleThreadContext` to create coroutine dispatcher backed by a worker.
  * `newFixedThreadPoolContext` to create coroutine dispatcher backed by a pool of `N` workers.
  * `Dispatchers.Main` backed by main queue on Darwin and by standalone worker on other platforms. **NOTE**: _Don't use `Dispatchers.Main` in unit-tests, because nothing is processing the main thread queue in unit-tests._
* `ktor`: `1.6.2-native-mm-eap-196` at https://maven.pkg.jetbrains.space/public/p/ktor/eap

### Using previous versions of the libraries

You can continue using previous versions of the libraries in your project.
Older versions (including `native-mt` for `kotlinx.coroutines`) could still be used, and the existing code will work just like with the previous MM.
The only known exception is creating Ktor HTTP client with default engine using `HttpClient()`. You will get 
```
kotlin.IllegalStateException: Failed to find HttpClientEngineContainer. Consider adding [HttpClientEngine] implementation in dependencies.
```
To fix this, specify the engine explicitly, by replacing `HttpClient()` with `HttpClient(Ios)` or other supported engine
(see [the documentation](https://ktor.io/docs/http-client-engines.html#native) for more details).

Other libraries might also have compatibility issues. If you encounter any, please report to the library authors.

## Performance issues

For the first preview we are using the simplest scheme for garbage collection: single-threaded stop-the-world
mark-and-sweep algorithm, which is triggered after enough functions, loop iterations and allocations were executed. This greatly hinders
the performance, and one of our top priorities now is addressing these performance problems.

We don't yet have nice instruments to monitor performance of the GC, so for now diagnosing requires looking at GC logs.
To enable the logs add compilation flag `-Xruntime-logs=gc=info` compiler. Or, with `gradle`:
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        freeCompilerArgs += "-Xruntime-logs=gc=info"
    }
}
```

Currently, the logs are only printed to stderr. **NOTE**: _the exact contents of the logs is subject to change._

A number of known performance issues:
* Since the collector is single-threaded stop-the-world, the pause time of every thread linearly depends on the number of
  objects in the heap. The more objects that are kept alive, the longer pauses will be. Large pauses on the main thread
  can result in laggy UI event handling. Both the pause time and the amount of objects in the heap are printed to the logs for each
  cycle of GC.
* Being stop-the-world also means that all threads with Kotlin/Native runtime active on them need to synchronize at the same
  time in order for the collection to begin. This also affects the pause time.
* There is a complicated relationship between Swift/ObjC objects and their Kotlin/Native counterparts, that causes Swift/ObjC objects
  to linger longer than necessary, which means that their Kotlin/Native counterparts are kept in the heap for longer, contributing
  to the slower collection time. This typically doesn't happen, but in some corner cases, for example, when
  there's a long loop, that on each iteration creates a number of temporary objects that cross the Swift/ObjC
  interop boundary (e.g. calling a kotlin callback from a loop in swift or vice versa).
  In the logs there's a number of stable refs in the root set. If this number keeps growing, it may indicate that Swift/ObjC objects
  are not being freed when they should.
  Try putting `autoreleasepool` around loop bodies (both Swift/ObjC and Kotlin) that do interop calls.
* Our GC triggers do not adapt to the workload: collection may be requested far more frequently than necessary, which means
  that GC time may dominate actually useful application run time and pause the threads more frequently than needed.
  This manifests in time between cycles being close (or even less) than the pause time. Both of these numbers are printed
  to the logs. Try increasing `kotlin.native.internal.GC.threshold` and `kotlin.native.internal.GC.thresholdAllocations` to force GC
  to happen less often. Note that, the exact meaning of `threshold` and `thresholdAllocations` may change in the future.
* Freezing is currently implemented suboptimally: internally a separate memory allocation may occur for each frozen object
  (this recursively includes the object subgraph), which puts unnecessary pressure on the heap.
* Unterminated `Worker`s and unconsumed `Future`s have objects pinned to the heap, which contributes to the pause time.
  Just like Swift/ObjC interop, this also manifests in a growing number of stable refs in the root set. To mitigate, look for
  `Worker.execute` methods being called with the resulting `Future` never being consumed (via `Future.consume` or `Future.result`) and
  make sure to either consume the `Future` or replace calls with `Worker.executeAfter` instead. Also look for `Worker`s that were
  `Worker.start`ed, but were never stopped via `Worker.requestTermination()` (also note that this call also returns a `Future`).
  And finally, make sure that `execute` and `executeAfter` is only called on `Worker`s that were `Worker.start`ed or if the receiving
  worker manually processes events with `Worker.processQueue`.

## Known bugs

* Compiler caches are not supported, so compilation of debug binaries will be slower.
* Freezing machinery is not thread-safe: if an object is being frozen on one thread, and its subgraph is being modified
  on another, by the end the object will be frozen, but some subgraph of it might be not.
* Documentation is not updated to reflect changes for the new MM.
* There's no handling of application state on iOS: if application goes into the background, the collector will not be
  throttled down; on the other hand the collection is not forced upon going into the background, which leaves
  the application with a larger memory footprint than necessary, making it a more likely target to be terminated by the OS.
* WASM (or indeed any target that does not have pthreads) is not supported with the new MM.

## Workarounds

### Unexpected object freezing
Some libraries might not be ready for the new memory model and freeze-transparency of `kotlinx.coroutines`, so unexpected `InvalidMutabilityException` 
or `FreezingException` might appear.
To workaround such cases, we added a `freezing` binary option that disables freezing fully (`disabled`) or 
partially (`explicitOnly`).
The former disables freezing mechanism at runtime (thus, making it no-op) while the latter disables automatic freezing of 
[`@SharedImmutable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/-shared-immutable/) globals, 
but keeps direct calls to [`freeze`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/freeze.html) fully functional. 
Similar to the [new memory model](#switch-to-the-new-memory-model), there are several ways to enable this option:
* In `gradle.properties`: 
```properties
kotlin.native.binary.freezing=disabled
```
* In Gradle build script:
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        binaryOptions["freezing"] = "disabled"
    }
}
```
* Compiler flag: 
```
-Xbinary=freezing=disabled
```
Please note that this option works only with the new MM.

If you want not just workaround the problem, but actually track down the source of the exceptions then
[`ensureNeverFrozen`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/ensure-never-frozen.html) is your best friend.

## Feedback

In our measurements we observed performance regressions with a slowdown up to a factor of 5.
If you observe anything much more significant, please report to [the performance meta issue](https://youtrack.jetbrains.com/issue/KT-48526).

Other problems encountered during migration to the new MM can be reported to [this meta issue](https://youtrack.jetbrains.com/issue/KT-48525).
