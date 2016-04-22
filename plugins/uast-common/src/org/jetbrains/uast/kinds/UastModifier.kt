/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
 * Uast declaration modifiers.
 *
 * @see UModifierOwner
 */
open class UastModifier(val name: String) {
    companion object {
        @JvmField
        val ABSTRACT = UastModifier("abstract")
        @JvmField
        val STATIC = UastModifier("static")
        @JvmField
        val FINAL = UastModifier("final")
        @JvmField
        val IMMUTABLE = UastModifier("immutable")
        @JvmField
        val VARARG = UastModifier("vararg")
        @JvmField
        val OVERRIDE = UastModifier("override")
        @JvmField
        val JVM_FIELD = UastModifier("field")

        // JVM-related modifiers are not listed here
        val VALUES = listOf(ABSTRACT, STATIC, FINAL, IMMUTABLE, VARARG, OVERRIDE)
    }

    override fun toString(): String{
        return "UastModifier(name='$name')"
    }
}
