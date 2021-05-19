/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import java.io.File
import java.io.ObjectInputStream
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode

internal fun File.deserializer(): GeneratedCode = Deserializer(this)

private class Deserializer(private val file: File) : GeneratedCode {

    override fun GeneratedCode.Builder.body() {
        import(File::class, ObjectInputStream::class)

        +"""
            object __Deserialization__ {
                val deserialized: Map<String, Any> by lazy { 
                    val serializedFile = File("${file.absolutePath}")
                    @Suppress("unchecked_cast")
                    ObjectInputStream(serializedFile.inputStream()).use { it.readObject() }  as Map<String, Any>
                }
                
                fun <T> unsafeReadSerializedValue(id: String): T {
                    @Suppress("unchecked_cast")
                    return deserialized[id] as T
                }
            }
        """.trimIndent()
    }

}