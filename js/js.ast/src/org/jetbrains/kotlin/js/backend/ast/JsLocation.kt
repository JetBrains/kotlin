/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.backend.ast

import java.io.Reader

data class JsLocation @JvmOverloads constructor(
    override val file: String,
    override val startLine: Int,
    override val startChar: Int,
    override val name: String? = null
) : JsLocationWithSource {
    override val fileIdentity: Any?
        get() = null
    override val sourceProvider: () -> Reader?
        get() = { null }

    override fun asSimpleLocation(): JsLocation = this
}

interface JsLocationWithSource {
    val file: String
    val startLine: Int
    val startChar: Int

    /**
     * The original name of the entity in the source code that this JS node was generated from.
     */
    val name: String?

    /**
     * An object to distinguish different files with the same paths
     */
    val fileIdentity: Any?
    val sourceProvider: () -> Reader?

    fun asSimpleLocation(): JsLocation
}

class JsLocationWithEmbeddedSource(
    private val location: JsLocation,
    override val fileIdentity: Any?,
    override val sourceProvider: () -> Reader?
) : JsLocationWithSource by location
