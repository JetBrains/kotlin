/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi.asm

import org.jetbrains.org.objectweb.asm.*

internal class ReplaceWithEmptyMethodVisitor(
    private val newMaxLocals: Int,
    private val delegate: MethodVisitor,
    api: Int
) : MethodVisitor(api, null) {
    override fun visitCode() {
        delegate.visitCode()
        delegate.visitMaxs(0, newMaxLocals)
        delegate.visitEnd()
    }

    override fun visitParameter(name: String?, access: Int) {
        delegate.visitParameter(name, access)
    }

    override fun visitParameterAnnotation(parameter: Int, desc: String?, visible: Boolean): AnnotationVisitor =
        delegate.visitParameterAnnotation(parameter, desc, visible)

    override fun visitAnnotationDefault(): AnnotationVisitor =
        delegate.visitAnnotationDefault()

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor =
        delegate.visitAnnotation(desc, visible)

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor =
        delegate.visitTypeAnnotation(typeRef, typePath, desc, visible)

    override fun visitAttribute(attr: Attribute?) {
        delegate.visitAttribute(attr)
    }
}