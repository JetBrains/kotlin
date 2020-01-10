/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.signature

import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.kaptlite.signature.ElementKind.*

data class ClassSignature(
    val typeParameters: List<SigTypeParameter>,
    val superClass: SigType,
    val interfaces: List<SigType>
)

data class MethodSignature(
    val typeParameters: List<SigTypeParameter>,
    val parameters: List<SigParameter>,
    val exceptionTypes: List<SigType>,
    val returnType: SigType
)

data class SigParameter(val name: String, val type: SigType)
data class SigTypeParameter(val name: String, val bounds: List<SigType>)

sealed class SigType {
    class Primitive private constructor(val javaName: String, val descriptor: Char) : SigType() {
        companion object {
            val VOID = Primitive("void", 'V')
            val BYTE = Primitive("byte", 'B')
            val SHORT = Primitive("short", 'S')
            val INT = Primitive("int", 'I')
            val LONG = Primitive("long", 'J')
            val BOOLEAN = Primitive("boolean", 'Z')
            val CHAR = Primitive("char", 'C')
            val FLOAT = Primitive("float", 'F')
            val DOUBLE = Primitive("double", 'D')

            private val ALL = listOf(VOID, BYTE, SHORT, INT, LONG, BOOLEAN, CHAR, FLOAT, DOUBLE)
                .map { it.descriptor to it }.toMap()

            fun get(descriptor: Char): Primitive = ALL.getValue(descriptor)
        }
    }

    class TypeVariable(val name: String) : SigType()
    class Array(val elementType: SigType) : SigType()
    class Class(val fqName: String) : SigType()
    class Nested(val outer: SigType, val name: String) : SigType()
    class Generic(val base: SigType, val args: List<SigTypeArgument>) : SigType()
}

val SigType.isJavaLangObject: Boolean
    get() = this is SigType.Class && this.fqName == Object::class.java.name

sealed class SigTypeArgument {
    object Unbound : SigTypeArgument()
    class Invariant(val type: SigType) : SigTypeArgument()
    class Extends(val type: SigType) : SigTypeArgument()
    class Super(val type: SigType) : SigTypeArgument()
}

object SignatureParser {
    fun parseClassSignature(signature: String): ClassSignature {
        val root = parse(signature)
        val sigTypeParameters = ArrayList<SignatureNode>(1)
        val sigSuperClasses = ArrayList<SignatureNode>(1)
        val sigInterfaces = ArrayList<SignatureNode>(1)
        root.split(sigTypeParameters, TypeParameter, sigSuperClasses, SuperClass, sigInterfaces, Interface)

        val typeParameters = sigTypeParameters.map { parseTypeParameter(it) }
        val superClass = parseType(sigSuperClasses.single().children.single())
        val interfaces = sigInterfaces.map { parseType(it.children.single()) }
        return ClassSignature(typeParameters, superClass, interfaces)
    }

    fun parseMethodSignature(
        signature: String,
        rawParameters: List<SigParameter>? = null,
        parameterTypeTransformer: (Int, () -> SigType) -> SigType = { _, f -> f() }
    ): MethodSignature {
        val root = parse(signature)
        val sigTypeParameters = ArrayList<SignatureNode>(1)
        val sigParameterTypes = ArrayList<SignatureNode>(2)
        val sigExceptionTypes = ArrayList<SignatureNode>(0)
        val sigReturnTypes = ArrayList<SignatureNode>(1)

        root.split(
            sigTypeParameters, TypeParameter,
            sigParameterTypes, ParameterType,
            sigExceptionTypes, ExceptionType,
            sigReturnTypes, ReturnType
        )

        val typeParameters = sigTypeParameters.map { parseTypeParameter(it) }

        val parameters = if (rawParameters != null) {
            assert(rawParameters.size >= sigParameterTypes.size)
            val offset = rawParameters.size - sigParameterTypes.size
            sigParameterTypes.mapIndexed { index, param ->
                val rawParameter = rawParameters[index + offset]
                val transformedType = parameterTypeTransformer(index) { parseType(param.children.single()) }
                SigParameter(rawParameter.name, transformedType)
            }
        } else {
            sigParameterTypes.mapIndexed { index, param ->
                SigParameter("p$index", parseType(param.children.single()))
            }
        }

        val exceptionTypes = sigExceptionTypes.map { parseType(it) }
        val returnType = parseType(sigReturnTypes.single().children.single())
        return MethodSignature(typeParameters, parameters, exceptionTypes, returnType)
    }

    fun parseFieldSignature(signature: String): SigType {
        val root = parse(signature)
        val superClass = root.children.single()
        assert(superClass.kind == SuperClass)
        return parseType(superClass.children.single())
    }

    private fun parseTypeParameter(node: SignatureNode): SigTypeParameter {
        assert(node.kind == TypeParameter)

        val sigClassBounds = ArrayList<SignatureNode>(1)
        val sigInterfaceBounds = ArrayList<SignatureNode>(1)
        node.split(sigClassBounds, ClassBound, sigInterfaceBounds, InterfaceBound)
        assert(sigClassBounds.size <= 1)

        val classBound = sigClassBounds.firstOrNull()?.let { parseBound(it) }?.takeIf { !it.isJavaLangObject }
        val interfaceBounds = sigInterfaceBounds.map { parseBound(it) }
        val allBounds = if (classBound != null) listOf(classBound) + interfaceBounds else interfaceBounds
        return SigTypeParameter(node.name!!, allBounds)
    }

    private fun parseBound(node: SignatureNode): SigType {
        assert(node.kind == ClassBound || node.kind == InterfaceBound)
        return parseType(node.children.single())
    }

    private fun parseType(node: SignatureNode): SigType {
        return when (node.kind) {
            ClassType -> {
                val typeArgs = mutableListOf<SignatureNode>()
                val innerClasses = mutableListOf<SignatureNode>()
                node.split(typeArgs, TypeArgument, innerClasses, InnerClass)

                val baseType = SigType.Class(node.name!!.replace('/', '.'))
                var type = parseGenericArgs(baseType, typeArgs)
                if (innerClasses.isEmpty()) return type

                for (innerClass in innerClasses) {
                    val nestedType = SigType.Nested(type, innerClass.name!!)
                    type = parseGenericArgs(nestedType, innerClass.children)
                }

                type
            }
            TypeVariable -> SigType.TypeVariable(node.name!!)
            ArrayType -> SigType.Array(parseType(node.children.single()))
            PrimitiveType -> SigType.Primitive.get(node.name!!.single())
            else -> error("Unsupported type: $node")
        }
    }

    private fun parseGenericArgs(base: SigType, args: List<SignatureNode>): SigType {
        if (args.isEmpty()) {
            return base
        }

        return SigType.Generic(base, args.map { arg ->
            assert(arg.kind == TypeArgument) { "Unexpected kind ${arg.kind}, $TypeArgument expected" }
            val variance = arg.name ?: return@map SigTypeArgument.Unbound

            val argType = parseType(arg.children.single())
            when (variance.single()) {
                '=' -> SigTypeArgument.Invariant(argType)
                '+' -> SigTypeArgument.Extends(argType)
                '-' -> SigTypeArgument.Super(argType)
                else -> error("Unknown variance, '=', '+' or '-' expected")
            }
        })
    }

    private fun parse(signature: String): SignatureNode {
        val parser = SignatureParserVisitor()
        SignatureReader(signature).accept(parser)
        return parser.root
    }
}

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