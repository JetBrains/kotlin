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

import org.jetbrains.kotlin.kapt.stubs.ElementKind.*
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
    val children: MutableList<SignatureNode> = SmartList()
}

class SignatureParser<Expression, TypeParameter>(
    private val converter: ParameterizedKaptStubConverter<*, Expression, *, *, *, *, TypeParameter, *, *, *>
) {
    class ClassGenericSignature<Expression, TypeParameter>(
        val typeParameters: List<TypeParameter>,
        val superClass: Expression,
        val interfaces: List<Expression>
    )

    class MethodGenericSignature<Expression, TypeParameter>(
        val typeParameters: List<TypeParameter>,
        val parameterTypes: List<Expression>,
        val exceptionTypes: List<Expression>,
        val returnType: Expression?,
    ) {
        fun withRefinedReturnType(newReturnType: Expression?) =
            if (newReturnType == null) this
            else MethodGenericSignature(
                typeParameters,
                parameterTypes,
                exceptionTypes,
                newReturnType
            )
    }

    fun parseClassSignature(signature: String): ClassGenericSignature<Expression, TypeParameter> {
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
        rawParameterTypes: List<Expression>,
        hasReturnType: Boolean,
        nonErrorParameterTypeProvider: (Int, () -> Expression) -> Expression
    ): MethodGenericSignature<Expression, TypeParameter> {
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
    ): Expression {
        val root = parse(signature)
        val superClass = root.children.single()
        assert(superClass.kind == SuperClass)

        return parseType(superClass.children.single())
    }

    private fun parseTypeParameter(node: SignatureNode): TypeParameter {
        assert(node.kind == TypeParameter)

        val classBounds = smartList()
        val interfaceBounds = smartList()
        node.split(classBounds, ClassBound, interfaceBounds, InterfaceBound)
        assert(classBounds.size <= 1)

        val parsedClassBound = classBounds.firstOrNull()?.let { parseBound(it) }
        val parsedInterfaceBounds = interfaceBounds.map { parseBound(it) }
        val allBounds = if (parsedClassBound != null) listOf(parsedClassBound) + parsedInterfaceBounds else parsedInterfaceBounds

        return converter.makeTypeParameter(node.name!!, allBounds)
    }

    private fun parseBound(node: SignatureNode): Expression {
        assert(node.kind == ClassBound || node.kind == InterfaceBound)
        return parseType(node.children.single())
    }

    private fun parseType(node: SignatureNode): Expression {
        val kind = node.kind
        return when (kind) {
            ClassType -> {
                val typeArgs = mutableListOf<SignatureNode>()
                val innerClasses = mutableListOf<SignatureNode>()
                node.split(typeArgs, TypeArgument, innerClasses, InnerClass)
                val convertedTypeArgs: List<Expression> = typeArgs.map { convertTypeArgument(it) }

                val fqName = converter.makeQualifiedName(node.name!!)
                var expression = makeExpressionForClassTypeWithArguments(fqName, convertedTypeArgs)

                if (innerClasses.isEmpty()) return expression

                for (innerClass in innerClasses) {
                    val convertedInnerClassArgs: List<Expression> =
                        innerClass.children.map { convertTypeArgument(it) }
                    expression = makeExpressionForClassTypeWithArguments(
                        converter.makeSelect(expression, innerClass.name!!),
                        convertedInnerClassArgs
                    )
                }

                expression
            }

            TypeVariable ->
                converter.makeSimpleName(node.name!!)
            ArrayType -> {
                val elementType = parseType(node.children.single())
                converter.makeArrayType(elementType, 1)
            }
            PrimitiveType ->
                converter.makePrimitiveType(node.name!!.single())

            else -> error("Unsupported type: $node")
        }
    }

    private fun makeExpressionForClassTypeWithArguments(
        fqNameExpression: Expression,
        args: List<Expression>,
    ): Expression {
        if (args.isEmpty()) return fqNameExpression
        return converter.makeTypeApply(fqNameExpression, args)
    }

    private fun convertTypeArgument(arg: SignatureNode): Expression {
        assert(arg.kind == TypeArgument) { "Unexpected kind ${arg.kind}, $TypeArgument expected" }

        val variance = arg.name ?: return converter.makeUnboundWildcard()
        val asmWildcard = variance.single()
        val argType = parseType(arg.children.single())
        return converter.makeWildcard(asmWildcard, argType)
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
