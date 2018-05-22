/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

internal class BinAnnData(
    val name: String,
    val args: ArrayList<String> = arrayListOf()
)

private class TemplateAnnotationVisitor(val anns: ArrayList<BinAnnData> = arrayListOf()) : AnnotationVisitor(Opcodes.ASM5) {
    override fun visit(name: String?, value: Any?) {
        anns.last().args.add(value.toString())
    }
}

private class TemplateClassVisitor(val annVisitor: TemplateAnnotationVisitor) : ClassVisitor(Opcodes.ASM5) {
    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
        val shortName = jvmDescToClassId(desc).shortClassName.asString()
        if (shortName.startsWith("KotlinScript")) {
            annVisitor.anns.add(BinAnnData(shortName))
        }
        return annVisitor
    }
}

private fun jvmDescToClassId(desc: String): ClassId {
    assert(desc.startsWith("L") && desc.endsWith(";")) { "Not a JVM descriptor: $desc" }
    val name = desc.substring(1, desc.length - 1)
    val cid = ClassId.topLevel(FqName(name.replace('/', '.')))
    return cid
}

internal fun loadAnnotationsFromClass(fileContents: ByteArray): ArrayList<BinAnnData> {

    val visitor =
        TemplateClassVisitor(TemplateAnnotationVisitor())

    ClassReader(fileContents).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

    return visitor.annVisitor.anns
}

