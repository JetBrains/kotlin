/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.org.objectweb.asm.*

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
    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        val shortName = Type.getType(desc).internalName.substringAfterLast("/")
        if (shortName.startsWith("KotlinScript")) {
            annVisitor.anns.add(BinAnnData(shortName))
            return annVisitor
        }
        return null
    }
}

internal fun loadAnnotationsFromClass(fileContents: ByteArray): ArrayList<BinAnnData> {

    val visitor =
        TemplateClassVisitor(TemplateAnnotationVisitor())

    ClassReader(fileContents).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

    return visitor.annVisitor.anns
}

