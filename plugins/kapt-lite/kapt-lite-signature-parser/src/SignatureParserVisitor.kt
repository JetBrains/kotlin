/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.signature

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor
import org.jetbrains.kaptlite.signature.ElementKind.*
import java.util.*

internal class SignatureParserVisitor : SignatureVisitor(Opcodes.API_VERSION) {
    val root = SignatureNode(Root)
    private val stack = ArrayDeque<SignatureNode>(5).apply { add(root) }

    private fun popUntil(kind: ElementKind?) {
        if (kind != null) {
            while (stack.peek().kind != kind) {
                stack.pop()
            }
        }
    }

    private fun popUntil(vararg kinds: ElementKind) {
        while (stack.peek().kind !in kinds) {
            stack.pop()
        }
    }

    private fun push(kind: ElementKind, parent: ElementKind? = null, name: String? = null) {
        popUntil(parent)

        val newNode = SignatureNode(kind, name)
        stack.peek().children += newNode
        stack.push(newNode)
    }

    override fun visitSuperclass(): SignatureVisitor {
        push(SuperClass, parent = Root)
        return super.visitSuperclass()
    }

    override fun visitInterface(): SignatureVisitor {
        push(Interface, parent = Root)
        return super.visitInterface()
    }

    override fun visitFormalTypeParameter(name: String) {
        push(TypeParameter, parent = Root, name = name)
    }

    override fun visitClassBound(): SignatureVisitor {
        push(ClassBound, parent = TypeParameter)
        return super.visitClassBound()
    }

    override fun visitInterfaceBound(): SignatureVisitor {
        push(InterfaceBound, parent = TypeParameter)
        return super.visitInterfaceBound()
    }

    override fun visitTypeArgument() {
        popUntil(ClassType, InnerClass)
        push(TypeArgument)
    }

    override fun visitTypeArgument(variance: Char): SignatureVisitor {
        popUntil(ClassType, InnerClass)
        push(TypeArgument, name = variance.toString())
        return super.visitTypeArgument(variance)
    }

    override fun visitInnerClassType(name: String) {
        push(InnerClass, name = name, parent = ClassType)
    }

    override fun visitParameterType(): SignatureVisitor {
        push(ParameterType, parent = Root)
        return super.visitParameterType()
    }

    override fun visitReturnType(): SignatureVisitor {
        push(ReturnType, parent = Root)
        return super.visitReturnType()
    }

    override fun visitExceptionType(): SignatureVisitor {
        push(ExceptionType, parent = Root)
        return super.visitExceptionType()
    }

    override fun visitClassType(name: String) {
        push(ClassType, name = name)
    }

    override fun visitTypeVariable(name: String) {
        push(TypeVariable, name = name)
    }

    override fun visitBaseType(descriptor: Char) {
        push(PrimitiveType, name = descriptor.toString())
    }

    override fun visitArrayType(): SignatureVisitor {
        push(ArrayType)
        return super.visitArrayType()
    }

    override fun visitEnd() {
        while (stack.peek().kind != ClassType) {
            stack.pop()
        }
        stack.pop()
    }
}
