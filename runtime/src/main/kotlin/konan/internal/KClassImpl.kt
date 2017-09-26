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

package konan.internal

import kotlin.reflect.KClass

@ExportForCompiler
internal class KClassImpl<T : Any>(private val typeInfo: NativePtr) : KClass<T> {
    override val simpleName: String?
        get() {
            val relativeName = getRelativeName(typeInfo)
                    ?: return null

            return relativeName.substringAfterLast(".")
        }

    override val qualifiedName: String?
        get() {
            val packageName = getPackageName(typeInfo)
                    ?: return null

            val relativeName = getRelativeName(typeInfo)!!
            return if (packageName.isEmpty()) {
                relativeName
            } else {
                "$packageName.$relativeName"
            }
        }

    override fun isInstance(value: Any?): Boolean = value != null && isInstance(value, this.typeInfo)

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && this.typeInfo == other.typeInfo

    override fun hashCode(): Int = typeInfo.hashCode()
}

@ExportForCompiler
@SymbolName("Kotlin_Any_getTypeInfo")
internal external fun getObjectTypeInfo(obj: Any): NativePtr

@ExportForCompiler
@Intrinsic
internal external inline fun <reified T : Any> getClassTypeInfo(): NativePtr

@SymbolName("Kotlin_TypeInfo_getPackageName")
private external fun getPackageName(typeInfo: NativePtr): String?

@SymbolName("Kotlin_TypeInfo_getRelativeName")
private external fun getRelativeName(typeInfo: NativePtr): String?

@SymbolName("Kotlin_TypeInfo_isInstance")
private external fun isInstance(obj: Any, typeInfo: NativePtr): Boolean
