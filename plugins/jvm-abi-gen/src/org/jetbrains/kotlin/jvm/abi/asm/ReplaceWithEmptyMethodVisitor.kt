/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi.asm

import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class ReplaceWithEmptyMethodVisitor(
    delegate: MethodVisitor,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions, api = Opcodes.API_VERSION) {
    override fun performTransformations(methodNode: MethodNode) {
        methodNode.instructions.clear()
        methodNode.localVariables.clear()
        methodNode.tryCatchBlocks.clear()
    }
}