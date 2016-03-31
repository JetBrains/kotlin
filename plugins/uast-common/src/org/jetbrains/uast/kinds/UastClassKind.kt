/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.uast

/**
 * Kinds of [UClass].
 */
open class UastClassKind(val text: String) {
    companion object {
        @JvmField
        val CLASS = UastClassKind("class")

        @JvmField
        val INTERFACE = UastClassKind("interface")

        @JvmField
        val ANNOTATION = UastClassKind("annotation")

        @JvmField
        val ENUM = UastClassKind("enum")

        @JvmField
        val OBJECT = UastClassKind("object")
    }

    override fun toString(): String{
        return "UastClassKind(text='$text')"
    }
}