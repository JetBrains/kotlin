/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import platform.darwin.NSObject
import kotlinx.cinterop.*
import kotlin.test.assertEquals

class Name : NSObject() {
    fun simpleName(): String? = this::class.simpleName
    fun qualifiedName(): String? = this::class.qualifiedName
}

@ExportObjCClass(name = "ExportedClass")
class ExportedName : NSObject() {
    fun simpleName(): String? = this::class.simpleName
    fun qualifiedName(): String? = this::class.qualifiedName
}

@ExportObjCClass()
class ExportedNoName : NSObject() {
    fun simpleName(): String? = this::class.simpleName
    fun qualifiedName(): String? = this::class.qualifiedName
}

fun main() {
    assertEquals("Name", Name().simpleName())
    assertEquals("Name", Name().qualifiedName())

    assertEquals("ExportedClass", ExportedName().simpleName())
    assertEquals("ExportedClass", ExportedName().qualifiedName())

    assertEquals("ExportedNoName", ExportedNoName().simpleName())
    assertEquals("ExportedNoName", ExportedNoName().qualifiedName())
}