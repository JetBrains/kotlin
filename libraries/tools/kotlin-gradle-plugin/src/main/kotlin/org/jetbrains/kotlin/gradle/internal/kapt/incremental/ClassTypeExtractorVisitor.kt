/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.org.objectweb.asm.*

private val toIgnore = setOf("java/lang/Object", "kotlin/Metadata", "org/jetbrains/annotations/NotNull")

class ClassTypeExtractorVisitor(visitor: ClassVisitor) : ClassVisitor(lazyAsmApiVersion.value, visitor) {

    private val abiTypes = mutableSetOf<String>()
    private val privateTypes = mutableSetOf<String>()

    private lateinit var classInternalName: String

    fun getAbiTypes() = abiTypes.filter { !toIgnore.contains(it) && it != classInternalName }.toSet()
    fun getPrivateTypes() = privateTypes.filter { !toIgnore.contains(it) && it != classInternalName && !abiTypes.contains(it) }.toSet()

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        classInternalName = name!!
        superName?.let {
            abiTypes.add(it)
        }
        interfaces?.let {
            abiTypes.addAll(it)
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val typeCollector = if (access and Opcodes.ACC_PRIVATE != 0) {
            privateTypes
        } else {
            abiTypes
        }

        desc?.also {
            val type = Type.getType(desc)

            maybeAdd(typeCollector, type.returnType)
            type.argumentTypes.forEach {
                maybeAdd(typeCollector, it)
            }
        }

        return MethodTypeExtractorVisitor(typeCollector, super.visitMethod(access, name, desc, signature, exceptions))
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        val typeCollector = if (access and Opcodes.ACC_PRIVATE != 0) {
            privateTypes
        } else {
            abiTypes
        }

        desc?.also {
            val type = Type.getType(desc)
            maybeAdd(typeCollector, type)
        }

        return FieldTypeExtractorVisitor(typeCollector, super.visitField(access, name, desc, signature, value))
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(abiTypes, Type.getType(it))
        }
        return AnnotationTypeExtractorVisitor(abiTypes, super.visitAnnotation(desc, visible))
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(abiTypes, Type.getType(it))
        }

        return AnnotationTypeExtractorVisitor(abiTypes, super.visitTypeAnnotation(typeRef, typePath, desc, visible))
    }
}

private class AnnotationTypeExtractorVisitor(private val typeCollector: MutableSet<String>, visitor: AnnotationVisitor?) :
    AnnotationVisitor(Opcodes.ASM5, visitor) {

    override fun visit(name: String?, value: Any?) {
        if (value is Type) {
            typeCollector.add(value.className)
        }
        super.visit(name, value)
    }

    override fun visitAnnotation(name: String?, desc: String?): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        return super.visitAnnotation(name, desc)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitArray(name))
    }

    override fun visitEnum(name: String?, desc: String?, value: String?) {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        super.visitEnum(name, desc, value)
    }
}

private class FieldTypeExtractorVisitor(private val typeCollector: MutableSet<String>, visitor: FieldVisitor?) :
    FieldVisitor(Opcodes.ASM5, visitor) {
    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitAnnotation(desc, visible))
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitTypeAnnotation(typeRef, typePath, desc, visible))
    }
}

private class MethodTypeExtractorVisitor(private val typeCollector: MutableSet<String>, visitor: MethodVisitor?) :
    MethodVisitor(Opcodes.ASM5, visitor) {

    override fun visitAnnotationDefault(): AnnotationVisitor {
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitAnnotationDefault())
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitAnnotation(desc, visible))
    }

    override fun visitParameterAnnotation(parameter: Int, desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }
        return AnnotationTypeExtractorVisitor(typeCollector, super.visitParameterAnnotation(parameter, desc, visible))
    }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? {
        desc?.let {
            maybeAdd(typeCollector, Type.getType(it))
        }

        return AnnotationTypeExtractorVisitor(typeCollector, super.visitTypeAnnotation(typeRef, typePath, desc, visible))
    }
}

private fun maybeAdd(set: MutableSet<String>, type: Type) {
    type.finalInternalName()?.let { set.add(it) }
}

private fun Type.finalInternalName(): String? {
    return when (this.sort) {
        Type.VOID, Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.DOUBLE -> null
        Type.ARRAY -> this.elementType.finalInternalName()
        Type.OBJECT -> this.internalName
        Type.METHOD -> null
        else -> null
    }
}