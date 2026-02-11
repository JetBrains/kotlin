/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.serialization.js.ast.AbstractSerializer
import org.jetbrains.kotlin.serialization.js.ast.AbstractSerializer.DataWriter
import java.io.DataOutputStream
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

public fun serializeTypeScriptFragment(output: File, fragment: TypeScriptDefinitionsFragment?) {
    if (fragment != null) {
        TypeScriptFragmentSerializer()
            .append(fragment)
            .saveTo(output.outputStream())
    } else {
        output.delete()
    }
}

context(serializer: AbstractSerializer)
public fun DataWriter.writeTypeScriptFragment(fragment: TypeScriptDefinitionsFragment) {
    writeString(fragment.raw)
    writeCollection(fragment.importedTypes.entries) { (classId, name) ->
        serializer.internalizeString(classId.asString())
        serializer.internalizeString(name)
    }
    writeCollection(fragment.exportedTypes.entries) { (classId, name) ->
        writeString(classId.asString())
        writeString(name)
    }
}


private class TypeScriptFragmentSerializer : AbstractSerializer() {
    private val fragmentSerializer = DataWriter()

    override fun DataOutputStream.serialize() {
        fragmentSerializer.saveTo(this)
    }

    fun append(fragment: TypeScriptDefinitionsFragment) = apply {
        fragmentSerializer.writeTypeScriptFragment(fragment)
    }
}
