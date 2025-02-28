/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * The marked `expect` declaration refines `expect` declaration from the closest transitive *dependsOn* [source set](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets).
 * The marked declaration is called a *refinement*.
 * The annotation can be put only on top-level `expect` declarations.
 *
 * A pair of an `expect` declaration and *refinement* declaration must obey the same rules
 * as a pair of regular `expect` and `actual` declarations.
 * Except, the `expect` *refinement* declaration must not have a body.
 * The rules are checked by the compiler.
 *
 * Example:
 * ```
 * // SOURCE SET: common
 * expect class PlatformSpecific {
 *     fun availableOnAllPlatforms()
 * }
 *
 * // SOURCE SET: native (dependsOn: common)
 * @ExpectRefinement
 * expect class PlatformSpecific { // `PlatformSpecific` in native source set refines `PlatformSpecific` in common source set
 *     fun availableOnAllPlatforms() // The declarations from the original expect declaration must be repeated
 *     fun availableOnlyOnNativePlatforms() // Add a new member
 * }
 *
 * // SOURCE SET: linux (dependsOn: native)
 * actual class PlatformSpecific {
 *     actual fun availableOnAllPlatforms() {}
 *     actual fun availableOnlyOnNativePlatforms() {}
 * }
 * ```
 *
 * An *refinement* declaration can be further refined by other *refinements*,
 * forming a chain of `expect` *refinement* declarations.
 * The chain must always end with an `actual` declaration.
 *
 * It's possible to have multiple *dependsOn* dependencies.
 * *dependsOn* relation forms a graph, it's not a tree.
 * Given that, a *refinement* must refine one and only one `expect` declaration.
 * The same applies to `actual` declarations.
 * An `actual` declaration must actualize one and only one `expect` declaration.
 *
 * ## Stability guarantees
 *
 * The feature is experimental.
 * **No stability guarantees are provided.**
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
@SinceKotlin("2.2")
@ExperimentalMultiplatform
public annotation class ExpectRefinement
