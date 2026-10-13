/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.Serializable

/** A source file that one or more elements of a [KdFragment] are declared in. */
@Serializable
data class KdFile(
    val name: String,
    /** Path relative to the root of `sources.jar` in the fragment; never an absolute file-system path. */
    val path: String,
    val language: KdSourceLanguage,
    /** Annotations present on the file itself, e.g. from a Java `package-info.java`. */
    val annotations: List<KdAnnotation>,
    val sourceInformation: Map<KdElementId, KdElementSourceInformation>,
)

/** The exact location of an element's declaration within a [KdFile], used for source links and for embedding source code. */
@Serializable
data class KdElementSourceInformation(
    val line: Int,
    val column: Int,
    /** An external URL pointing to the declaration's source, e.g. on GitHub. */
    val url: String?,
)

/** The language a [KdFile] is written in. */
@Serializable
enum class KdSourceLanguage { KOTLIN, JAVA }
