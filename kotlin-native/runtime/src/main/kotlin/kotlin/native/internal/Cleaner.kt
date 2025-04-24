/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.*
import kotlin.native.runtime.GC

@Deprecated("Use kotlin.native.ref.Cleaner instead.", ReplaceWith("kotlin.native.ref.Cleaner"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public interface Cleaner

/**
 * Creates an object with a cleanup associated.
 *
 * After the resulting object ("cleaner") gets deallocated by memory manager,
 * [block] is eventually called once with [argument].
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
 * It is not specified which thread runs [block], as well as whether two or more
 * blocks from different cleaners can be run in parallel.
 *
 * Note: if [argument] refers (directly or indirectly) the cleaner, then both
 * might leak, and the [block] will not be called in this case.
 * For example, the code below has a leak:
 * ```
 * class LeakingResourceWrapper {
 *     private val resource = Resource()
 *     private val cleaner = createCleaner(this) { it.resource.dispose() }
 * }
 * ```
 * In this case cleaner's argument (`LeakingResourceWrapper`) can't be deallocated
 * until cleaner's block is executed, which can happen only strictly after
 * the cleaner is deallocated, which can't happen until `LeakingResourceWrapper`
 * is deallocated. So the requirements on object deallocations are contradictory
 * in this case, which can't be handled gracefully. The cleaner's block
 * is not executed then, and cleaner and its argument might leak
 * (depending on the implementation).
 *
 * [block] should not use `@ThreadLocal` globals, because it may
 * be executed on a different thread.
 *
 * If [block] throws an exception, the behavior is unspecified.
 *
 * Cleaners cannot be used to perform actions during the program shutdown:
 * * cleaners that are referenced from globals will not be garbage collected at all,
 * * cleaners that become unreferenced just before exiting main() might not be garbage collected,
     because the GC might not get a chance to run.
 *
 * @param argument must be shareable
 * @param block must not capture anything
 */
// TODO: Consider just annotating the lambda argument rather than hardcoding checking
// by function name in the compiler.
@Deprecated("Use kotlin.native.ref.createCleaner instead.", ReplaceWith("kotlin.native.ref.createCleaner(argument, block)"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@Suppress("DEPRECATION_ERROR")
@ExperimentalStdlibApi
@ExportForCompiler
@OptIn(ExperimentalNativeApi::class)
public fun <T> createCleaner(argument: T, block: (T) -> Unit): Cleaner =
        kotlin.native.ref.createCleanerImpl(argument, block) as Cleaner
