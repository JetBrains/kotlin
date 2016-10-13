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
@file:JvmName("UastBinaryExpressionWithTypeUtils")
package org.jetbrains.uast

/**
 * Kinds of [UBinaryExpressionWithType].
 * Examples: type casts, instance checks.
 */
open class UastBinaryExpressionWithTypeKind(val name: String) {
    open class TypeCast(name: String) : UastBinaryExpressionWithTypeKind(name)
    open class InstanceCheck(name: String) : UastBinaryExpressionWithTypeKind(name)

    companion object {
        @JvmField
        val TYPE_CAST = TypeCast("as")

        @JvmField
        val INSTANCE_CHECK = InstanceCheck("is")

        @JvmField
        val UNKNOWN = UastBinaryExpressionWithTypeKind("<unknown>")
    }
}