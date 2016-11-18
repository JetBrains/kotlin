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

package org.jetbrains.kotlin.kapt3

import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree.*
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor
import java.util.*
import org.jetbrains.kotlin.kapt3.ElementKind.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import com.sun.tools.javac.util.List as JavacList

/*
    Root (Class)
        * FormalTypeParameter
        + SuperClass
        * Interface

    Root (Method)
        * TypeParameter
        * ParameterType
        + ReturnType
        * ExceptionType

    Root (Field)
        + SuperClass

    TypeParameter < Root
        + ClassBound
        * InterfaceBound

    ParameterType < Root
        + Type

    ReturnType < Root
        + Type

    Type :: ClassType | TypeVariable | PrimitiveType | ArrayType

    ClassBound < TypeParameter
        + ClassType

    InterfaceBound < TypeParameter
        ? ClassType
        ? TypeVariable

    TypeVariable < InterfaceBound

    SuperClass < TopLevel
        + ClassType

    Interface < TopLevel
        + ClassType

    ClassType < *
        * TypeArgument

    TypeArgument < ClassType
        + ClassType
 */

enum class ElementKind {
    Root, TypeParameter, ClassBound, InterfaceBound, SuperClass, Interface, TypeArgument, ParameterType, ReturnType, ExceptionType,
    ClassType, TypeVariable, PrimitiveType, ArrayType
}

class SignatureNode(val kind: ElementKind, val name: String? = null) {
    val children: MutableList<SignatureNode> = SmartList<SignatureNode>()
}

class SignatureParser(val treeMaker: KaptTreeMaker) {
    class ClassGenericSignature(
            val typeParameters: JavacList<JCTypeParameter>,
            val superClass: JCExpression,
            val interfaces: JavacList<JCExpression>)

    class MethodGenericSignature(
            val typeParameters: JavacList<JCTypeParameter>,
            val parameterTypes: JavacList<JCVariableDecl>,
            val exceptionTypes: JavacList<JCExpression>,
            val returnType: JCExpression?
    )
    
    fun parseClassSignature(
            signature: String?,
            rawSuperClass: JCExpression,
            rawInterfaces: JavacList<JCExpression>
    ): ClassGenericSignature {
        if (signature == null) {
            return ClassGenericSignature(JavacList.nil(), rawSuperClass, rawInterfaces)
        }

        val root = parse(signature)
        val typeParameters = smartList()
        val superClasses = smartList()
        val interfaces = smartList()
        root.split(typeParameters, TypeParameter, superClasses, SuperClass, interfaces, Interface)

        val jcTypeParameters = mapValues(typeParameters) { parseTypeParameter(it) }
        val jcSuperClass = parseType(superClasses.single().children.single())
        val jcInterfaces = mapValues(interfaces) { parseType(it.children.single()) }
        return ClassGenericSignature(jcTypeParameters, jcSuperClass, jcInterfaces)
    }

    fun parseMethodSignature(
            signature: String?,
            rawParameters: JavacList<JCVariableDecl>,
            rawExceptionTypes: JavacList<JCExpression>,
            rawReturnType: JCExpression?
    ): MethodGenericSignature {
        if (signature == null) {
            return MethodGenericSignature(JavacList.nil(), rawParameters, rawExceptionTypes, rawReturnType)
        }

        val root = parse(signature)
        val typeParameters = smartList()
        val parameterTypes = smartList()
        val exceptionTypes = smartList()
        val returnTypes = smartList()
        root.split(typeParameters, TypeParameter, parameterTypes, ParameterType, exceptionTypes, ExceptionType, returnTypes, ReturnType)

        val jcTypeParameters = mapValues(typeParameters) { parseTypeParameter(it) }
        assert(rawParameters.size >= parameterTypes.size)
        val offset = rawParameters.size - parameterTypes.size
        val jcParameters = mapValuesIndexed(parameterTypes) { index, it ->
            val rawParameter = rawParameters[index + offset]
            treeMaker.VarDef(rawParameter.modifiers, rawParameter.getName(), parseType(it.children.single()), rawParameter.initializer)
        }
        val jcExceptionTypes = mapValues(exceptionTypes) { parseType(it) }
        val jcReturnType = if (rawReturnType == null) null else parseType(returnTypes.single().children.single())
        return MethodGenericSignature(jcTypeParameters, jcParameters, jcExceptionTypes, jcReturnType)
    }

    fun parseFieldSignature(
            signature: String?,
            rawType: JCExpression
    ): JCExpression {
        if (signature == null) return rawType

        val root = parse(signature)
        val superClass = root.children.single()
        assert(superClass.kind == SuperClass)

        return parseType(superClass.children.single())
    }

    private fun parseTypeParameter(node: SignatureNode): JCTypeParameter {
        assert(node.kind == TypeParameter)

        val classBounds = smartList()
        val interfaceBounds = smartList()
        node.split(classBounds, ClassBound, interfaceBounds, InterfaceBound)
        assert(classBounds.size <= 1)

        val jcClassBound = classBounds.firstOrNull()?.let { parseBound(it) }
        val jcInterfaceBounds = mapValues(interfaceBounds) { parseBound(it) }
        val allBounds = if (jcClassBound != null) jcInterfaceBounds.prepend(jcClassBound) else jcInterfaceBounds
        return treeMaker.TypeParameter(treeMaker.name(node.name!!), allBounds)
    }

    private fun parseBound(node: SignatureNode): JCExpression {
        assert(node.kind == ClassBound || node.kind == InterfaceBound)
        return parseType(node.children.single())
    }

    private fun parseType(node: SignatureNode): JCExpression {
        val kind = node.kind
        return when (kind) {
            ClassType -> {
                val classFqName = node.name!!.replace('/', '.')
                val args = node.children
                val fqNameExpression = treeMaker.convertFqName(classFqName)
                if (args.isEmpty()) return fqNameExpression

                treeMaker.TypeApply(fqNameExpression, mapValues(args) { arg ->
                    assert(arg.kind == TypeArgument) { "Unexpected kind ${arg.kind}, $TypeArgument expected" }
                    val variance = arg.name ?: return@mapValues treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)

                    val argType = parseType(arg.children.single())
                    when (variance.single()) {
                        '=' -> argType
                        '+' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), argType)
                        '-' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), argType)
                        else -> error("Unknown variance, '=', '+' or '-' expected")
                    }
                })
            }
            TypeVariable -> treeMaker.convertSimpleName(node.name!!)
            ArrayType -> treeMaker.TypeArray(parseType(node.children.single()))
            PrimitiveType -> {
                val typeTag = when (node.name!!.single()) {
                    'V' -> TypeTag.VOID
                    'Z' -> TypeTag.BOOLEAN
                    'C' -> TypeTag.CHAR
                    'B' -> TypeTag.BYTE
                    'S' -> TypeTag.SHORT
                    'I' -> TypeTag.INT
                    'F' -> TypeTag.FLOAT
                    'J' -> TypeTag.LONG
                    'D' -> TypeTag.DOUBLE
                    else -> error("Illegal primitive type ${node.name}")
                }
                treeMaker.TypeIdent(typeTag)
            }
            else -> error("Unsupported type: $node")
        }
    }

    private fun parse(signature: String): SignatureNode {
        val parser = SignatureParserVisitor()
        SignatureReader(signature).accept(parser)
        return parser.root
    }
}

private fun smartList() = SmartList<SignatureNode>()

private fun SignatureNode.split(l1: MutableList<SignatureNode>, e1: ElementKind, l2: MutableList<SignatureNode>, e2: ElementKind) {
    for (child in children) {
        val kind = child.kind
        when (kind) {
            e1 -> l1 += child
            e2 -> l2 += child
            else -> error("Unknown kind: $kind")
        }
    }
}

private fun SignatureNode.split(
        l1: MutableList<SignatureNode>,
        e1: ElementKind,
        l2: MutableList<SignatureNode>,
        e2: ElementKind,
        l3: MutableList<SignatureNode>,
        e3: ElementKind
) {
    for (child in children) {
        val kind = child.kind
        when (kind) {
            e1 -> l1 += child
            e2 -> l2 += child
            e3 -> l3 += child
            else -> error("Unknown kind: $kind")
        }
    }
}

private fun SignatureNode.split(
        l1: MutableList<SignatureNode>,
        e1: ElementKind,
        l2: MutableList<SignatureNode>,
        e2: ElementKind,
        l3: MutableList<SignatureNode>,
        e3: ElementKind,
        l4: MutableList<SignatureNode>,
        e4: ElementKind
) {
    for (child in children) {
        val kind = child.kind
        when (kind) {
            e1 -> l1 += child
            e2 -> l2 += child
            e3 -> l3 += child
            e4 -> l4 += child
            else -> error("Unknown kind: $kind")
        }
    }
}

private class SignatureParserVisitor : SignatureVisitor(Opcodes.ASM5) {
    val root = SignatureNode(Root)
    private val stack = ArrayDeque<SignatureNode>(5).apply { add(root) }

    private fun push(kind: ElementKind, parent: ElementKind? = null, name: String? = null) {
        if (parent != null) {
            while (stack.peek().kind != parent) {
                stack.pop()
            }
        }

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
        push(TypeArgument, parent = ClassType)
    }

    override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
        push(TypeArgument, parent = ClassType, name = wildcard.toString())
        return super.visitTypeArgument(wildcard)
    }

    override fun visitClassType(name: String) {
        push(ClassType, name = name)
    }

    override fun visitTypeVariable(name: String) {
        push(TypeVariable, name = name)
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