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

data class JsLocation(
        override val file: String,
        override val startLine: Int,
        override val startChar: Int
) : JsLocationWithSource {
    override val identityObject: Any? = null
    override val sourceProvider: () -> Reader? = { null }

    override fun asSimpleLocation(): JsLocation = this
}

interface JsLocationWithSource {
    val file: String
    val startLine: Int
    val startChar: Int
    val identityObject: Any?
    val sourceProvider: () -> Reader?

    fun asSimpleLocation(): JsLocation
}

class JsLocationWithEmbeddedSource(
        private val location: JsLocation,
        override val identityObject: Any?,
        override val sourceProvider: () -> Reader?
) : JsLocationWithSource by location