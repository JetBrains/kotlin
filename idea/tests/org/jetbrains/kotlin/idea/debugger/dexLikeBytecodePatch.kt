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

package org.jetbrains.kotlin.idea.debugger

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

fun patchDexTests(dir: File) {
    dir.listFiles({ file -> file.isDirectory && file.name.startsWith("dex") }).forEach { dir ->
        dir.listFiles { testOutputFile -> testOutputFile.extension == "class" }.forEach { classFile ->
            applyDexLikePatch(classFile)
        }
    }
}

private fun applyDexLikePatch(file: File) {
    file.copyTo(File(file.absolutePath + ".temp"))

    val reader = ClassReader(file.readBytes())
    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

    val visitor = writer
            .withRemoveSourceDebugExtensionVisitor()

    reader.accept(visitor, 0)

    file.writeBytes(writer.toByteArray())
}

private fun ClassVisitor.withRemoveSourceDebugExtensionVisitor(): ClassVisitor {
    return object : ClassVisitor(Opcodes.ASM5, this) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, null)
        }
    }
}