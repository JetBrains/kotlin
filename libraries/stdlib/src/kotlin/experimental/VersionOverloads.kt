/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * Specifies at which version an optional parameter is introduced to a function. This also instructs the compiler to generate
 * earlier-version, deprecated overloads of the function that have fewer numbers of optional parameters.
 *
 * Given a function that has parameters introduced at N unique version numbers (for example, version "1" to "N"), N-1 overloads are generated:
 * the first overload only has parameters without the annotation,
 * the second overload has all the first overload's parameters and the parameters introduced at version "1",
 * and so on until the (N-1)-th overload, which has all parameters up to version "N-1" and unannotated parameters.
 *
 * These overloads are deprecated with hidden visibility, meaning that they are mainly for the purpose of maintaining binary compatibility.
 *
 * If a data class constructor has parameters annotated with the annotation,
 * the compiler also generates the corresponding overloads of its `copy` method in addition to the constructor overloads.
 *
 * @property version version string. Parsed as an Apache Maven comparable version.
 */
@Target(VALUE_PARAMETER)
@MustBeDocumented
@ExperimentalStdlibApi
public annotation class IntroducedAt(val version: String)