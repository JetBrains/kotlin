/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.js.AbstractDeserializer
import java.io.File

public fun deserializeTypeScriptFragment(input: File): TypeScriptDefinitionsFragment {
    return TypeScriptFragmentDeserializer(input.readBytes()).readTypeScriptFragment()
}

public fun AbstractDeserializer.readTypeScriptFragment(): TypeScriptDefinitionsFragment {
    val raw = readString()
    val importedTypes = readMap { readClassId() to stringTable[readInt()] }
    val exportedTypes = readMap { readClassId() to stringTable[readInt()] }
    return TypeScriptDefinitionsFragment(raw, importedTypes, exportedTypes)
}

private fun AbstractDeserializer.readClassId(): ClassId =
    ClassId.fromString(stringTable[readInt()])

private class TypeScriptFragmentDeserializer(source: ByteArray) : AbstractDeserializer(source) {
}