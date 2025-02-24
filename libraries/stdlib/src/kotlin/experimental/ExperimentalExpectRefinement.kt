/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * The annotation can be put only on top-level `expect` declarations.
 * The marked `expect` declarations refine "the original" `expect` declarations from the "closest" transitive "dependsOn" module.
 *
 * A pair of "the original" `expect` declaration and a refined `expect` declaration must obey the same rules
 * as a pair of regular `expect` and `actual` declarations.
 * Except, the refined `expect` declaration must not have a body.
 * The rules are checked by the compiler.
 *
 * A refined `expect` declaration can be further refined by other `expect` declaration forming a chain of refined `expect` declarations.
 * The chain must always end with the `actual` declaration.
 *
 * It's possible to have multiple "dependsOn" dependencies.
 * "dependsOn" relation forms a graph; unfortunately, it's not a tree.
 * Given that, Refined `expect` must refine one and only one `expect` declaration.
 * The same applies to `actual` declarations.
 * An `actual` declaration must actualize one and only one `expect` declaration.
 *
 * Example:
 * ```
 * // MODULE: common
 * expect class Foo {
 *     fun foo()
 * }
 *
 * // MODULE: native (dependsOn: common)
 * @ExperimentalExpectRefinement
 * expect class Foo { // `Foo` in native module "refines" `Foo` in common module
 *     fun foo() // The declarations from the original expect declaration must be repeated
 *     fun bar() // Add new member
 * }
 *
 * // MODULE: linux (dependsOn: native)
 * actual class Foo {
 *     actual fun foo() {}
 *     actual fun bar() {}
 * }
 * ```
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
public annotation class ExperimentalExpectRefinement
