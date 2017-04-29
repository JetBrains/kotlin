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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipFile

internal fun loadCompilerVersion(compilerJar: File): String {
    var result = "<unknown>"

    try {
        ZipFile(compilerJar).use { file ->
            val fileName = KotlinCompilerVersion::class.java.name.replace('.', '/') + ".class"
            val bytes = file.getInputStream(file.getEntry(fileName)).use { inputStream -> inputStream.readBytes() }
            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
                    if (name == KotlinCompilerVersion::VERSION.name && value is String) {
                        result = value
                    }
                    return super.visitField(access, name, desc, signature, value)
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
        }
    }
    catch (e: Throwable) {}

    return result
}
