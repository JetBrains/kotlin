/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

/**
 * Marker for declarations that depend on numeric types of different bit width on at least two platforms.
 *
 * @param actualPlatformTypes Contains platform types represented as `{konanTarget}: {type fqn}`
 * e.g. ["linux_x64: kotlin.Int", "linux_arm64: kotlin.Long"]
 */
@Suppress("unused") // Is emitted by the Commonizer
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
        message = "The declaration is using numbers with different bit widths in least two actual platforms. " +
                "Such types shall not be used in user-defined 'expect fun' signatures",
        level = RequiresOptIn.Level.ERROR
)
public annotation class UnsafeNumber(val actualPlatformTypes: Array<String>)

/**
 * Marks Objective-C and Swift interoperability API as Beta.
 *
 * The marked API has official Beta stability level, is considered to be a vital part of Kotlin/Native
 * and Kotlin Multiplatform, and is evolved in a backward-compatible manner with the proper migration paths for incompatible changes.
 *
 * It may partially lack the concise semantics, documentation, and API to interoperate with Swift and Objective-C features
 * that do not have a direct Kotlin counterpart.
 *
 * This API is recommended to be used for interoperability purposes, but with the API availability scope confined and narrowed down.
 */
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class BetaInteropApi

/**
 * Marks foreign-language-related API as experimental.
 *
 * Foreign API includes all operations and classifiers that are required for operating with
 * unmanaged and foreign memory, including but not limited to such declarations as [CPointer], [CPointed], [StableRef], and `Pinned`.
 * It also includes API of C and Objective-C libraries, except for those that are available in Kotlin by default
 * (like `platform.posix.*` or `platform.Foundation.*`).
 *
 * Such API is considered experimental and has the following known limitations and caveats:
 * - It is either undocumented or lacks extensive and sound description.
 * - There is no clear behavioural semantics and explicit mapping between foreign (e.g. C-pointer) concepts
 *     and the corresponding foreign API (e.g. [CPointer]). Such declarations might have an unsound mapping.
 * - There is no clear semantic difference between similar declarations.
 * - It lacks best practices and cookbook-like recommendations.
 *
 * ABI and API compatibilities are provided on a best-effort basis.
 * We also do provide a best-effort migration path for binary/source incompatible changes, including the proper deprecation
 * cycle, migration path, and introducing replacements for the API rather than API breakage.
 *
 * This API is recommended to be used for interoperability purposes, but with the API availability scope confined and narrowed down.
 */
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
public annotation class ExperimentalForeignApi
