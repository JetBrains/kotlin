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

package org.jetbrains.kotlin.kapt.stubs

import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree.*
import org.jetbrains.kotlin.kapt.base.getJavacList
import org.jetbrains.kotlin.kapt.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt.stubs.ElementKind.*
import org.jetbrains.kotlin.kapt.util.appendListIfNonEmpty
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor
import java.util.*

/*
    Root (Class)
        * TypeParameter
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
        ! ClassType

    Interface < TopLevel
        ! ClassType

    ClassType < *
        * TypeArgument
        * InnerClass

    InnerClass < ClassType
        ! TypeArgument

    TypeArgument < ClassType | InnerClass
        + ClassType
 */

internal enum class ElementKind {
    Root, TypeParameter, ClassBound, InterfaceBound, SuperClass, Interface, TypeArgument, ParameterType, ReturnType, ExceptionType,
    ClassType, InnerClass, TypeVariable, PrimitiveType, ArrayType
}

private class SignatureNode(val kind: ElementKind, val name: String? = null) {
    val children: MutableList<SignatureNode> = SmartList<SignatureNode>()
}

class SignatureParser(private val treeMaker: KaptTreeMaker) {
    class ClassGenericSignature(
        val typeParameters: List<Pair<JCTypeParameter, String>>,
        val superClass: Pair<JCExpression, String>,
        val interfaces: List<Pair<JCExpression, String>>
    )

    class MethodGenericSignature(
        val typeParameters: List<Pair<JCTypeParameter, String>>,
        val parameterTypes: List<Pair<JCExpression, String>>,
        val exceptionTypes: List<Pair<JCExpression, String>>,
        val returnType: Pair<JCExpression, String>?,
    ) {
        fun withRefinedReturnType(newReturnType: Pair<JCExpression, String>?) =
            if (newReturnType == null) this
            else MethodGenericSignature(
                typeParameters,
                parameterTypes,
                exceptionTypes,
                newReturnType
            )
    }

    fun parseClassSignature(signature: String): ClassGenericSignature {
        val root = parse(signature)
        val typeParameters = smartList()
        val superClasses = smartList()
        val interfaces = smartList()
        root.split(typeParameters, TypeParameter, superClasses, SuperClass, interfaces, Interface)

        val parsedTypeParameters = typeParameters.map { parseTypeParameter(it) }
        val superClass = parseType(superClasses.single().children.single())
        val parsedInterfaces = interfaces.map { parseType(it.children.single()) }
        return ClassGenericSignature(parsedTypeParameters, superClass, parsedInterfaces)
    }

    fun parseMethodSignature(
        signature: String,
        rawParameterTypes: List<Pair<JCExpression, String>>,
        hasReturnType: Boolean,
        nonErrorParameterTypeProvider: (Int, () -> Pair<JCExpression, String>) -> Pair<JCExpression, String>
    ): MethodGenericSignature {
        val root = parse(signature)
        val typeParameters = smartList()
        val parameterTypes = smartList()
        val exceptionTypes = smartList()
        val returnTypes = smartList()
        root.split(typeParameters, TypeParameter, parameterTypes, ParameterType, exceptionTypes, ExceptionType, returnTypes, ReturnType)

        val parsedTypeParameters = typeParameters.map { parseTypeParameter(it) }
        assert(rawParameterTypes.size >= parameterTypes.size)
        val offset = rawParameterTypes.size - parameterTypes.size
        val parsedParameterTypes = rawParameterTypes.take(offset) + parameterTypes.mapIndexed { index, it ->
            nonErrorParameterTypeProvider(index) { parseType(it.children.single()) }
        }
        val parsedExceptionTypes = exceptionTypes.map { parseType(it) }
        val returnType = if (hasReturnType) parseType(returnTypes.single().children.single()) else null
        return MethodGenericSignature(parsedTypeParameters, parsedParameterTypes, parsedExceptionTypes, returnType)
    }

    fun parseFieldSignature(
        signature: String,
    ): Pair<JCExpression, String> {
        val root = parse(signature)
        val superClass = root.children.single()
        assert(superClass.kind == SuperClass)

        return parseType(superClass.children.single())
    }

    private fun parseTypeParameter(node: SignatureNode): Pair<JCTypeParameter, String> {
        assert(node.kind == TypeParameter)

        val classBounds = smartList()
        val interfaceBounds = smartList()
        node.split(classBounds, ClassBound, interfaceBounds, InterfaceBound)
        assert(classBounds.size <= 1)

        val parsedClassBound = classBounds.firstOrNull()?.let { parseBound(it) }
        val jcClassBound = parsedClassBound?.first
        val parsedInterfaceBounds = interfaceBounds.map { parseBound(it) }
        val jcInterfaceBounds = parsedInterfaceBounds.getJavacList()
        val allBounds = if (jcClassBound != null) jcInterfaceBounds.prepend(jcClassBound) else jcInterfaceBounds

        val text = buildString {
            append(node.name!!)
            if (allBounds.isNotEmpty()) {
                append(" extends ")
            }
            if (parsedClassBound != null) {
                append(parsedClassBound.second)
                append(" & ")
            }
            for (bound in parsedInterfaceBounds) {
                append(bound.second)
                append(" & ")
            }
            if (allBounds.isNotEmpty()) {
                setLength(length - " & ".length)
            }
        }
        return treeMaker.TypeParameter(treeMaker.name(node.name!!), allBounds) to text
    }

    private fun parseBound(node: SignatureNode): Pair<JCExpression, String> {
        assert(node.kind == ClassBound || node.kind == InterfaceBound)
        return parseType(node.children.single())
    }

    private fun parseType(node: SignatureNode): Pair<JCExpression, String> {
        val kind = node.kind
        return when (kind) {
            ClassType -> {
                val typeArgs = mutableListOf<SignatureNode>()
                val innerClasses = mutableListOf<SignatureNode>()
                node.split(typeArgs, TypeArgument, innerClasses, InnerClass)
                val convertedTypeArgs: List<Pair<JCExpression, String>> = typeArgs.map { convertTypeArgument(it) }

                val sb = StringBuilder()
                sb.append(treeMaker.getQualifiedName(node.name!!))
                sb.appendTypeArguments(convertedTypeArgs)
                var expression = makeExpressionForClassTypeWithArguments(treeMaker.FqName(node.name), convertedTypeArgs)
                if (innerClasses.isEmpty()) return expression to sb.toString()

                for (innerClass in innerClasses) {
                    val convertedInnerClassArgs: List<Pair<JCExpression, String>> =
                        innerClass.children.map { convertTypeArgument(it) }
                    expression = makeExpressionForClassTypeWithArguments(
                        treeMaker.Select(expression, treeMaker.name(innerClass.name!!)),
                        convertedInnerClassArgs
                    )
                    sb.append(".").append(innerClass.name)
                    sb.appendTypeArguments(convertedInnerClassArgs)
                }

                expression to sb.toString()
            }

            TypeVariable -> treeMaker.SimpleName(node.name!!) to node.name
            ArrayType -> {
                val elementType = parseType(node.children.single())
                treeMaker.TypeArray(elementType.first) to "${elementType.second}[]"
            }
            PrimitiveType -> {
                when (node.name!!.single()) {
                    'V' -> treeMaker.TypeIdent(TypeTag.VOID) to "void"
                    'Z' -> treeMaker.TypeIdent(TypeTag.BOOLEAN) to "boolean"
                    'C' -> treeMaker.TypeIdent(TypeTag.CHAR) to "char"
                    'B' -> treeMaker.TypeIdent(TypeTag.BYTE) to "byte"
                    'S' -> treeMaker.TypeIdent(TypeTag.SHORT) to "short"
                    'I' -> treeMaker.TypeIdent(TypeTag.INT) to "int"
                    'F' -> treeMaker.TypeIdent(TypeTag.FLOAT) to "float"
                    'J' -> treeMaker.TypeIdent(TypeTag.LONG) to "long"
                    'D' -> treeMaker.TypeIdent(TypeTag.DOUBLE) to "double"
                    else -> error("Illegal primitive type ${node.name}")
                }
            }

            else -> error("Unsupported type: $node")
        }
    }

    private fun makeExpressionForClassTypeWithArguments(
        fqNameExpression: JCExpression,
        args: List<Pair<JCExpression, String>>,
    ): JCExpression {
        if (args.isEmpty()) return fqNameExpression

        return treeMaker.TypeApply(fqNameExpression, args.getJavacList())
    }

    private fun convertTypeArgument(arg: SignatureNode): Pair<JCExpression, String> {
        assert(arg.kind == TypeArgument) { "Unexpected kind ${arg.kind}, $TypeArgument expected" }

        val variance = arg.name ?: return treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null) to "?"
        val argType = parseType(arg.children.single())
        return when (variance.single()) {
            '=' -> argType
            '+' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), argType.first) to
                    "? extends ${argType.second}"
            '-' -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), argType.first) to
                    "? super ${argType.second}"
            else -> error("Unknown variance, '=', '+' or '-' expected")
        }
    }

    private fun StringBuilder.appendTypeArguments(typeArgs: List<Pair<JCExpression, String>>) =
        appendListIfNonEmpty(typeArgs, "<", ">") { it.second }

    private fun parse(signature: String): SignatureNode {
        val parser = SignatureParserVisitor()
        SignatureReader(signature).accept(parser)
        return parser.root
    }
}

private fun smartList() = SmartList<SignatureNode>()

private fun SignatureNode.split(l1: MutableList<SignatureNode>, e1: ElementKind, l2: MutableList<SignatureNode>, e2: ElementKind) {
    for (child in children) {
        when (val kind = child.kind) {
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
        when (val kind = child.kind) {
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
        when (val kind = child.kind) {
            e1 -> l1 += child
            e2 -> l2 += child
            e3 -> l3 += child
            e4 -> l4 += child
            else -> error("Unknown kind: $kind")
        }
    }
}

private class SignatureParserVisitor : SignatureVisitor(Opcodes.API_VERSION) {
    val root = SignatureNode(Root)
    private val stack = ArrayDeque<SignatureNode>(5).apply { add(root) }

    private fun popUntil(kind: ElementKind?) {
        if (kind != null) {
            while (stack.peek().kind != kind) {
                stack.pop()
            }
        }
    }

    private fun popUntil(kinds: Collection<ElementKind>) {
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
        popUntil(listOf(ClassType, InnerClass))
        push(TypeArgument)
    }

    override fun visitTypeArgument(variance: Char): SignatureVisitor {
        popUntil(listOf(ClassType, InnerClass))
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
