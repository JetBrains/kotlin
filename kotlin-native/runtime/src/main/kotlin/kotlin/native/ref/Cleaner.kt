/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.ref

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.isShareable
import kotlin.native.concurrent.freeze
import kotlin.native.internal.*
import kotlinx.cinterop.NativePtr

/**
 * The marker interface for objects that have a cleanup action associated with them.
 *
 * Use [createCleaner] to create an instance of this type.
 */
@ExperimentalNativeApi
@SinceKotlin("1.9")
public sealed interface Cleaner

/**
 * Creates a [Cleaner] object that runs [cleanupAction] with given [resource] some time after its deallocation.
 *
 * Example of usage:
 * ```
 * class ResourceWrapper {
 *     private val resource = Resource()
 *
 *     private val cleaner = createCleaner(resource) { it.dispose() }
 * }
 * ```
 *
 * When `ResourceWrapper` becomes unused and gets deallocated, its `cleaner`
 * is also deallocated, and the resource is disposed later.
 *
 * It is not specified which thread runs [cleanupAction], as well as whether two or more
 * cleanup actions from different cleaners can be run in parallel.
 *
 * Note: if [resource] refers (directly or indirectly) the cleaner, then both
 * might leak, and the [cleanupAction] will not be called in this case.
 * For example, the code below has a leak:
 * ```
 * class LeakingResourceWrapper {
 *     private val resource = Resource()
 *     private val cleaner = createCleaner(this) { it.resource.dispose() }
 * }
 * ```
 * In this case cleaner's argument (`LeakingResourceWrapper`) can't be deallocated
 * until [cleanupAction] (`it.resource.dispose()`) is executed, which can happen only strictly after
 * the cleaner is deallocated, which can't happen until `LeakingResourceWrapper`
 * is deallocated. So the requirements on object deallocations are contradictory
 * in this case, which can't be handled gracefully. The cleanup action
 * is not executed then, and cleaner and its argument might leak
 * (depending on the implementation).
 * The same problem occures when [cleanupAction] captures a value that refers (directly or indirectly) the cleaner:
 * ```
 * class LeakingResourceWrapper {
 *     private val cleaner = createCleaner(...) {
 *         doSomething()
 *         ...
 *     }
 *
 *     private fun doSomething() {
 *         ...
 *     }
 * }
 * ```
 * In the example above the cleanup lambda implicitly captures `this` object to call `doSomething()`.
 *
 * [cleanupAction] should not use `@ThreadLocal` globals, because it may
 * be executed on a different thread.
 *
 * If [cleanupAction] throws an exception, the behavior is unspecified.
 *
 * Cleaners cannot be used to perform actions during the program shutdown:
 * * cleaners that are referenced from globals will not be garbage collected at all,
 * * cleaners that become unreferenced just before exiting main() might not be garbage collected,
     because the GC might not get a chance to run.
 *
 * @param resource an object for which to perform [cleanupAction]
 * @param cleanupAction a cleanup to perform on [resource]. Must not capture anything.
 */
// TODO: Consider just annotating the lambda argument rather than hardcoding checking
// by function name in the compiler.
@ExperimentalNativeApi
@SinceKotlin("1.9")
@ExportForCompiler
public fun <T> createCleaner(resource: T, cleanupAction: (resource: T) -> Unit): Cleaner =
        createCleanerImpl(resource, cleanupAction)

@ExperimentalNativeApi
@OptIn(FreezingIsDeprecated::class)
internal fun <T> createCleanerImpl(resource: T, cleanupAction: (T) -> Unit): Cleaner {
    if (!resource.isShareable())
        throw IllegalArgumentException("$resource must be shareable")

    val clean = {
        // TODO: Maybe if this fails with exception, it should be (optionally) reported.
        cleanupAction(resource)
    }.freeze()

    // Make sure there's an extra reference to clean, so it's definitely alive when CleanerImpl is destroyed.
    val cleanPtr = createStablePointer(clean)

    // Make sure cleaner worker is initialized.
    getCleanerWorker()

    return CleanerImpl(cleanPtr).freeze()
}

@Suppress("DEPRECATION")
@ExperimentalNativeApi
@NoReorderFields
@ExportTypeInfo("theCleanerImplTypeInfo")
@HasFinalizer
private class CleanerImpl(
        private val cleanPtr: NativePtr,
): Cleaner, kotlin.native.internal.Cleaner {}


@GCUnsafeCall("CreateStablePointer")
external private fun createStablePointer(obj: Any): NativePtr