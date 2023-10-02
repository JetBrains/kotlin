/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package androidx.compose.compiler.plugins.kotlin

var uniqueNumber = 0

fun loadClass(loader: ClassLoader, name: String?, bytes: ByteArray): Class<*> {
    val defineClassMethod = ClassLoader::class.javaObjectType.getDeclaredMethod(
        "defineClass",
        String::class.javaObjectType,
        ByteArray::class.javaObjectType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
    )
    defineClassMethod.isAccessible = true
    return defineClassMethod.invoke(loader, name, bytes, 0, bytes.size) as Class<*>
}
