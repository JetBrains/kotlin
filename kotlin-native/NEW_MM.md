# New memory manager migration guide

> The new memory manager is Alpha. It's not production-ready and may be changed at any time.
> Opt-in is required (see the details below), and you should use it only for evaluation purposes. We would appreciate your feedback on it in [YouTrack](https://youtrack.jetbrains.com/issue/KT-48525).

In the new memory manager (MM), we're lifting restrictions on object sharing: there's no need to freeze objects to share them
between threads anymore.

In particular:
* Top-level properties can be accessed and modified by any thread without using `@SharedImmutable`.
* Objects passing through interop can be accessed and modified by any thread without freezing them.
* `Worker.executeAfter` no longer requires `operation` to be frozen.
* `Worker.execute` no longer requires `producer` to return an isolated object subgraph.
* Reference cycles containing `AtomicReference` and `FreezableAtomicReference` do not cause memory leaks.

A few precautions:
* As with the previous MM, memory is not reclaimed eagerly: an object is reclaimed only when GC happens. This extends to Swift/ObjC objects that crossed interop boundary into Kotlin/Native.
* Prior to 1.6.20 `AtomicReference` from `kotlin.native.concurrent` still required freezing the `value`, and we suggested using `FreezableAtomicReference` instead.
  Starting with 1.6.20 `AtomicReference` on the new MM behaves exactly like `FreezableAtomicReference`.
  Alternatively, you can use `AtomicRef` from `atomicfu`. _Note that `atomicfu` has not reached 1.x yet_.
* `deinit` on Swift/ObjC objects (and the objects they refer to) will be called on a different thread if these objects cross interop boundary into Kotlin/Native.
* When calling Kotlin suspend functions from Swift, completion handlers might be called on threads other than the main.

The new MM also brings another set of changes:
* Global properties are initialized lazily when the file they are defined in is first accessed. Previously global properties were initialized at the program startup. This is in line with Kotlin/JVM.
  As a workaround, properties that must be initialized at the program start can be marked with `@EagerInitialization`. Before using, consult the [`@EagerInitialization`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native/-eager-initialization/) documentation.
* `by lazy {}` properties support thread-safety modes and do not handle unbounded recursion. This is in line with Kotlin/JVM.
* Exceptions that escape `operation` in `Worker.executeAfter` are processed like in other parts of the runtime: by trying to execute a user-defined unhandled exception hook or terminating the program if the hook was not found or failed with an exception itself.
* In the new MM, freezing is deprecated, disabled by default, and will be removed in one of the future releases.
  Don't use freezing if you don't need your code to work with the old MM.

## Enable the new MM

### Update the Kotlin/Native compiler

Update to Kotlin 1.6.20 or newer.

### Switch to the new MM

Add the compilation flag `-Xbinary=memoryModel=experimental`. In Gradle, you can alternatively do one of the following:

* In `gradle.properties`:
```properties
kotlin.native.binary.memoryModel=experimental
```

* In a Gradle build script:
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        binaryOptions["memoryModel"] = "experimental"
    }
}
```

If `kotlin.native.isExperimentalMM()` returns `true`, you've successfully enabled the new MM.

### Update the libraries

To take full advantage of the new MM, we released new versions of the following libraries:
* `kotlinx.coroutines`: `1.6.0` or newer (will automatically detect when running with the new memory manager).
    * No freezing. Every common primitive (Channels, Flows, coroutines) works through `Worker` boundaries.
    * Unlike the `native-mt` version, library objects are transparent for `freeze`. For example, if you freeze a channel, all of its internals will get frozen, so it won't work as expected. In particular, this can happen when freezing something that captures a channel.
    * `Dispatchers.Default` is backed by a pool of `Worker`s on Linux and Windows and by a global queue on Apple targets.
    * `newSingleThreadContext` to create a coroutine dispatcher that is backed by a `Worker`.
    * `newFixedThreadPoolContext` to create a coroutine dispatcher backed by a pool of `N` `Worker`s.
    * `Dispatchers.Main` is backed by the main queue on Darwin and by a standalone `Worker` on other platforms.
      _In unit tests, nothing processes the main thread queue, so do not use `Dispatchers.Main` in unit tests unless it was mocked, which can be done by calling `Dispatchers.setMain` from `kotlinx-coroutines-test`._
* `ktor`: `2.0.0` or newer.

### Using previous versions of the libraries

In your project, you can continue using previous versions of the libraries (including `native-mt` for `kotlinx.coroutines`). The existing code will work just like with the previous MM. The only known exception is creating a Ktor HTTP client with the default engine using `HttpClient()`. In this case, you get the following error:

```
kotlin.IllegalStateException: Failed to find HttpClientEngineContainer. Consider adding [HttpClientEngine] implementation in dependencies.
```

To fix this, specify the engine explicitly by replacing `HttpClient()` with `HttpClient(Ios)` or other supported engines
(see the [Ktor documentation](https://ktor.io/docs/http-client-engines.html#native) for more details).

Other libraries might also have compatibility issues. If you encounter any, report to the library authors.

Known issues:
* SQLDelight: https://github.com/cashapp/sqldelight/issues/2556

## Performance issues

For the first preview, we're using the simplest scheme for garbage collection: single-threaded stop-the-world
mark-and-sweep algorithm, which is triggered after enough functions, loop iterations, and allocations were executed. This greatly hinders the performance, and one of our top priorities now is addressing these performance issues.

We don't have nice instruments to monitor the GC performance yet. So far, diagnosing requires looking at GC logs. To enable the logs, add the compilation flag `-Xruntime-logs=gc=info` in a Gradle build script:
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        freeCompilerArgs += "-Xruntime-logs=gc=info"
    }
}
```

Currently, the logs are only printed to stderr. _Note that the exact contents of the logs will change._

The list of known performance issues:

* Since the collector is single-threaded stop-the-world, the pause time of every thread linearly depends on the number of objects in the heap. The more objects that are kept alive, the longer the pauses are. Long pauses on the main thread can result in laggy UI event handling. Both the pause time and the number of objects in the heap are printed to the logs for each GC cycle.
* Being stop-the-world also means that all threads with Kotlin/Native runtime active on them need to synchronize simultaneously for the collection to begin. This also affects the pause time.
* There is a complicated relationship between Swift/ObjC objects and their Kotlin/Native counterparts, which causes Swift/ObjC objects to linger longer than necessary. It means that their Kotlin/Native counterparts are kept in the heap longer, contributing to the slower collection time. This typically doesn't happen, but in some corner cases, for example, when a long loop creates several temporary objects that cross the Swift/ObjC interop boundary on each iteration (for example, calling a Kotlin callback from a loop in Swift or vice versa).
  In the logs, there's a number of stable refs in the root set. If this number keeps growing, it may indicate that the Swift/ObjC objects are not being freed when they should. Try putting `autoreleasepool` around loop bodies (both in Swift/ObjC and Kotlin) that do interop calls.
* [(YouTrack issue)](https://youtrack.jetbrains.com/issue/KT-48537) Our GC triggers do not adapt to the workload: collections may be requested far more frequently than necessary, which means that GC time may dominate useful application run time and pause the threads more frequently than needed.
  This manifests in time between cycles being close (or even less) than the pause time. Both of these numbers are printed to the logs.
  Try increasing `kotlin.native.internal.GC.threshold` and `kotlin.native.internal.GC.thresholdAllocations` to force GC to happen less often. Note that the exact meaning of `threshold` and `thresholdAllocations` may change in the future.
* Freezing is currently implemented suboptimally: internally, a separate memory allocation may occur for each frozen object (this recursively includes the object subgraph), which puts unnecessary pressure on the heap.
* Unterminated `Worker`s and unconsumed `Future`s have objects pinned to the heap, contributing to the pause time. Like Swift/ObjC interop, this also manifests in a growing number of stable refs in the root set.
  To mitigate:
    * Look for calls to `Worker.execute` with the resulting `Future` objects that are never consumed using `Future.consume` or `Future.result`.
      Make sure to either consume all `Future` objects or replace these calls with `Worker.executeAfter` instead.
    * Look for `Worker`s that were `Worker.start`ed, but never stopped via `Worker.requestTermination()` (also, note that this call also returns a `Future`).
    * Make sure that `execute` and `executeAfter` are only called on `Worker`s that were `Worker.start`ed or if the receiving `Worker` manually processes events with `Worker.processQueue`.

In some of our measurements we observed performance regressions with a slowdown up to a factor of 5. In some other cases we observed performance improvements instead.
If you observe regressions more significant than 5x, please report to [this performance meta issue](https://youtrack.jetbrains.com/issue/KT-48526).

## Known bugs

* Compiler caches are not supported, so the compilation of debug binaries will be slower.
* Freezing machinery is not thread-safe: if an object is being frozen on one thread, and its subgraph is being modified on another, by the end, the object will be frozen, but some subgraph of it might be not.
* Documentation is not updated to reflect changes for the new MM.
* There's no application state handling on iOS: the collector will not be throttled down if the application goes into the background.
  However, the collection is not forced upon going into the background, which leaves the application with a larger memory footprint than necessary, making it a more likely target to be terminated by the OS.
* WASM (or any target that doesn't have pthreads) is not supported with the new MM.


## Workarounds

### Unexpected object freezing

Some libraries might not be ready for the new MM and freeze-transparency of `kotlinx.coroutines`, so unexpected `InvalidMutabilityException` or `FreezingException` might appear.

To workaround such cases, we added a `freezing` binary option that disables freezing fully (`disabled`) or partially (`explicitOnly`).
The former disables the freezing mechanism at runtime (thus, making it a no-op), while the latter disables automatic freezing of
[`@SharedImmutable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/-shared-immutable/) globals, but keeps direct calls to [`freeze`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/freeze.html) fully functional.

To enable this, add the compilation flag `-Xbinary=freezing=disabled`. In Gradle, you can alternatively do one of the following:

* In `gradle.properties`:
```properties
kotlin.native.binary.freezing=disabled
```
* In a Gradle build script:
```kotlin
// build.gradle.kts

kotlin.targets.withType(KotlinNativeTarget::class.java) {
    binaries.all {
        binaryOptions["freezing"] = "disabled"
    }
}
```

> **NOTE**: this option works only with the new MM.

If you want not just workaround the problem, but track down the source of the exceptions, then [`ensureNeverFrozen`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/ensure-never-frozen.html) is your best friend.

## Feedback

If you encounter performance regressions with a slowdown of more than a factor of 5, report to [this performance meta issue](https://youtrack.jetbrains.com/issue/KT-48526).

You can report other issues with migration to the new MM to [this meta issue](https://youtrack.jetbrains.com/issue/KT-48525).
