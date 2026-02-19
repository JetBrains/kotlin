/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * This annotation marks the experimental API for function overloading.
 *
 * > Beware using the annotated API especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 *
 * Any usage of a declaration annotated with `@ExperimentalVersionOverloading` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalVersionOverloading::class)`,
 * or by using the compiler argument `-opt-in=kotlin.ExperimentalVersionOverloading`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.3")
public annotation class ExperimentalVersionOverloading

/**
 * Instructs the Kotlin compiler to generate overloads by specifying the versions in which _optional_ parameters were
 * introduced, after the initial publication of the library.
 * Generated overloads are hidden and aimed to maintain binary compatibility,
 * they let users of your library upgrade to the newer version without having to recompile their code.
 *
 * Value parameters with the same version are assumed to have been introduced at the same time.
 * For example, given the signature,
 *
 * ```kotlin
 * fun example(a: A, @IntroducedAt("1.1") b: B = defaultB, @IntroducedAt("1.1") c: C = defaultC, @IntroducedAt("1.4") d: D = defaultD)
 * ```
 *
 * the compiler generates overloads corresponding to the following,
 *
 * ```kotlin
 * fun example(a: A) // hidden, initial version
 * fun example(a: A, @IntroducedAt("1.1") b: B = defaultB, @IntroducedAt("1.1") c: C = defaultC) // hidden, version 1.1
 *
 * // non-hidden overload, version 1.4
 * fun example(a: A, @IntroducedAt("1.1") b: B = defaultB, @IntroducedAt("1.1") c: C = defaultC, @IntroducedAt("1.4") d: D = defaultD)
 * ```
 *
 * Note that once a user of the library compiles against the new version, the most recent overload
 * (the one with all the optional parameters) is chosen for code generation. That means that from that
 * point on the older version of the library may no longer be used as a dependency.
 *
 * ### Restrictions on `IntroducedAt`
 *
 * The version string must follow the [Apache Maven version format](https://maven.apache.org/ref/3-LATEST/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html).
 * Versions in a signature must appear in **ascending** order.
 *
 * - Parameters without this annotation are assumed to correspond to the "initial version".
 * - If the last parameter has a function type, this rule may be disregarded.
 *   This makes it possible to introduce optional parameters before the **trailing lambda** position.
 *
 * It is not allowed to use this annotation on extension receivers or context parameters,
 * only in value parameters.
 *
 * Value parameters marked with the annotation must have a **default value**.
 * Default values for optional arguments may not depend on values from a newer version.
 *
 * It is not allowed to use this annotation on non-final methods, including abstract methods and interfaces.
 * These restrictions are similar to those of [kotlin.jvm.JvmName].
 *
 * It is not allowed to use this annotation in combination with [kotlin.jvm.JvmOverloads].
 *
 * If a data class constructor has parameters annotated with the annotation,
 * the compiler also generates the corresponding overloads of its `copy` method in addition to the constructor overloads.
 *
 * > For more information you can check the [full design (KEEP) document](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0431-version-overloading.md).
 *
 * @property version version string following the [Apache Maven version format](https://maven.apache.org/ref/3-LATEST/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@SinceKotlin("2.3")
@ExperimentalVersionOverloading
public annotation class IntroducedAt(val version: String)