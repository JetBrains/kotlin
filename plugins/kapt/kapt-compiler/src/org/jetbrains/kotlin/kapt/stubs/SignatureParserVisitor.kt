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

import org.jetbrains.kotlin.kapt.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt.stubs.ElementKind.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.TypePath
import org.jetbrains.org.objectweb.asm.TypeReference
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

    Root (Standalone type)
        + Type

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

class SignatureParser<Expression, Annotation : Expression, TypeParameter>(
    private val converter: ParameterizedKaptStubConverter<*, Expression, *, *, *, Annotation, TypeParameter, *, *, *>
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
                newReturnType,
            )
    }

    internal fun parseClassSignature(
        signature: String,
        typeAnnotations: List<TypeAnnotation<Annotation>> = emptyList(),
    ): ClassGenericSignature<Expression, TypeParameter> {
        val root = parse(signature)
        val typeParameters = smartList()
        val superClasses = smartList()
        val interfaces = smartList()
        root.split(typeParameters, TypeParameter, superClasses, SuperClass, interfaces, Interface)

        val parsedTypeParameters = parseTypeParameters(
            typeParameters, typeAnnotations, TypeReference.CLASS_TYPE_PARAMETER, TypeReference.CLASS_TYPE_PARAMETER_BOUND,
        )

        fun parseSuperType(node: SignatureNode, superTypeAnnotationIndex: Int): Expression {
            val type = node.children.single()
            val annotations = typeAnnotations.forTarget(TypeReference.newSuperTypeReference(superTypeAnnotationIndex))
            return parseType(type, mapTypeAnnotationsToNodes(annotations, type))
        }

        val superClass = parseSuperType(superClasses.single(), SUPERCLASS_TYPE_ANNOTATION_INDEX)
        val parsedInterfaces = interfaces.mapIndexed { index, node -> parseSuperType(node, index) }
        return ClassGenericSignature(parsedTypeParameters, superClass, parsedInterfaces)
    }

    internal fun parseMethodSignature(
        signature: String,
        rawParameterTypes: List<Expression>,
        hasReturnType: Boolean,
        typeAnnotations: List<TypeAnnotation<Annotation>>,
        varargParameterIndex: Int?,
        nonErrorParameterTypeProvider: (Int, () -> Expression) -> Expression,
    ): MethodGenericSignature<Expression, TypeParameter> {
        val root = parse(signature)
        val typeParameters = smartList()
        val parameterTypes = smartList()
        val exceptionTypes = smartList()
        val returnTypes = smartList()
        root.split(typeParameters, TypeParameter, parameterTypes, ParameterType, exceptionTypes, ExceptionType, returnTypes, ReturnType)

        val parsedTypeParameters = parseTypeParameters(
            typeParameters, typeAnnotations, TypeReference.METHOD_TYPE_PARAMETER, TypeReference.METHOD_TYPE_PARAMETER_BOUND,
        )

        fun parseTypeWithTargetAnnotations(node: SignatureNode, reference: TypeReference, isVararg: Boolean = false): Expression {
            val type = node.children.single()
            return parseType(type, mapTypeAnnotationsToNodes(typeAnnotations.forTarget(reference), type), isVararg)
        }

        assert(rawParameterTypes.size >= parameterTypes.size)
        val offset = rawParameterTypes.size - parameterTypes.size
        val parsedParameterTypes = rawParameterTypes.take(offset) + parameterTypes.mapIndexed { index, it ->
            val parameterIndex = index + offset
            val isVararg = parameterIndex == varargParameterIndex
            if (isVararg) {
                check(it.children.single().kind == ArrayType) { "A vararg parameter must have an array signature" }
            }
            nonErrorParameterTypeProvider(parameterIndex) {
                parseTypeWithTargetAnnotations(it, TypeReference.newFormalParameterReference(parameterIndex), isVararg)
            }
        }
        val parsedExceptionTypes = exceptionTypes.mapIndexed { index, node ->
            parseTypeWithTargetAnnotations(node, TypeReference.newExceptionReference(index))
        }
        val returnType = if (hasReturnType) {
            parseTypeWithTargetAnnotations(returnTypes.single(), TypeReference.newTypeReference(TypeReference.METHOD_RETURN))
        } else null
        return MethodGenericSignature(parsedTypeParameters, parsedParameterTypes, parsedExceptionTypes, returnType)
    }

    internal fun parseTypeSignature(
        signature: String,
        typeAnnotations: List<Pair<TypePath?, Annotation>> = emptyList(),
        isVararg: Boolean = false,
    ): Expression {
        val parser = SignatureParserVisitor(converter.treeMaker::resolveQualifiedName)
        SignatureReader(signature).acceptType(parser)
        val type = parser.root.children.single()
        return parseType(type, mapTypeAnnotationsToNodes(typeAnnotations, type), isVararg)
    }

    private fun mapTypeAnnotationsToNodes(
        typeAnnotations: List<Pair<TypePath?, Annotation>>,
        root: SignatureNode,
    ): IdentityHashMap<SignatureNode, MutableList<Annotation>> {
        val annotationsByNode = IdentityHashMap<SignatureNode, MutableList<Annotation>>()
        for ([typePath, annotation] in typeAnnotations) {
            val target = root.findTypePathTarget(typePath)
            if (target == null) {
                // The backend can emit annotation paths that are not matched to signature parts due to known and unknown bugs
                converter.kaptContext.logger.warn(
                    "Type annotation $annotation at path '$typePath' has no anchor in the generic signature and will not be emitted"
                )
                continue
            }
            annotationsByNode.getOrPut(target, ::SmartList).add(annotation)
        }
        return annotationsByNode
    }

    private fun parseTypeParameters(
        parameters: List<SignatureNode>,
        typeAnnotations: List<TypeAnnotation<Annotation>>,
        parameterSort: Int,
        boundSort: Int,
    ): List<TypeParameter> = parameters.mapIndexed { index, parameter ->
        val annotations = typeAnnotations.forTarget(TypeReference.newTypeParameterReference(parameterSort, index))
            .map { [path, annotation] ->
                require(path == null || path.length == 0) { "A type parameter annotation cannot have a type path: $path" }
                annotation
            }
        val boundAnnotations = typeAnnotations.filter {
            it.reference.sort == boundSort && it.reference.typeParameterIndex == index
        }
        parseTypeParameter(parameter, annotations, boundAnnotations)
    }

    private fun parseTypeParameter(
        node: SignatureNode,
        annotations: List<Annotation> = emptyList(),
        boundTypeAnnotations: List<TypeAnnotation<Annotation>> = emptyList(),
    ): TypeParameter {
        assert(node.kind == TypeParameter)

        val classBounds = smartList()
        val interfaceBounds = smartList()
        node.split(classBounds, ClassBound, interfaceBounds, InterfaceBound)
        assert(classBounds.size <= 1)

        val annotationsByBoundIndex = boundTypeAnnotations.groupBy(
            { it.reference.typeParameterBoundIndex },
            { it.path to it.annotation },
        )
        val parsedClassBound = classBounds.firstOrNull()?.let { parseBound(it, annotationsByBoundIndex[0].orEmpty()) }
        // Class and type-variable bounds have index 0, indices for interface bounds starts from 1 - see
        // `AnnotationCodegen.genAnnotationsOnTypeParametersAndBounds`
        var nextInterfaceIndex = 1
        val parsedInterfaceBounds = interfaceBounds.map { bound ->
            val boundIndex = if (bound.children.single().kind == TypeVariable) 0 else nextInterfaceIndex++
            parseBound(bound, boundIndex.let { annotationsByBoundIndex[it] }.orEmpty())
        }
        val allBounds = if (parsedClassBound != null) listOf(parsedClassBound) + parsedInterfaceBounds else parsedInterfaceBounds

        return converter.makeTypeParameter(node.name!!, allBounds, annotations)
    }

    private fun parseBound(node: SignatureNode, annotations: List<Pair<TypePath?, Annotation>>): Expression {
        assert(node.kind == ClassBound || node.kind == InterfaceBound)
        val type = node.children.single()
        return parseType(type, mapTypeAnnotationsToNodes(annotations, type))
    }

    private fun parseType(
        node: SignatureNode,
        annotationsByNode: Map<SignatureNode, List<Annotation>>,
        isVararg: Boolean = false,
    ): Expression {
        val kind = node.kind
        val annotationsOnCurrentType = annotationsByNode[node].orEmpty()
        val expression = when (kind) {
            ClassType -> {
                val typeArgs = mutableListOf<SignatureNode>()
                val innerClasses = mutableListOf<SignatureNode>()
                node.split(typeArgs, TypeArgument, innerClasses, InnerClass)
                val convertedTypeArgs: List<Expression> = typeArgs.map { convertTypeArgument(it, annotationsByNode) }

                val classType = converter.makeClassType(node.name!!, annotationsOnCurrentType)
                var expression = makeExpressionForClassTypeWithArguments(classType, convertedTypeArgs)

                for (innerClass in innerClasses) {
                    val convertedInnerClassArgs: List<Expression> =
                        innerClass.children.map { convertTypeArgument(it, annotationsByNode) }
                    val annotationsOnInnerClass = annotationsByNode[innerClass].orEmpty()
                    val selectedInnerType = converter.makeInnerClassType(
                        expression,
                        innerClass.name!!,
                        annotationsOnInnerClass,
                    )
                    expression = makeExpressionForClassTypeWithArguments(
                        selectedInnerType,
                        convertedInnerClassArgs
                    )
                }

                expression
            }

            TypeVariable ->
                converter.makeSimpleName(node.name!!)
            ArrayType -> {
                val arrayDimensions = mutableListOf<List<Annotation>>()
                var elementNode = node
                while (elementNode.kind == ArrayType) {
                    arrayDimensions += annotationsByNode[elementNode].orEmpty()
                    elementNode = elementNode.children.single()
                }
                converter.makeArrayType(parseType(elementNode, annotationsByNode), arrayDimensions, isVararg)
            }
            PrimitiveType ->
                converter.makePrimitiveType(node.name!!.single())

            else -> error("Unsupported type: $node")
        }

        return if (annotationsOnCurrentType.isEmpty() || kind == ClassType || kind == ArrayType) {
            expression
        } else {
            converter.makeAnnotatedType(expression, annotationsOnCurrentType)
        }
    }

    private fun makeExpressionForClassTypeWithArguments(
        fqNameExpression: Expression,
        args: List<Expression>,
    ): Expression {
        if (args.isEmpty()) return fqNameExpression
        return converter.makeTypeApply(fqNameExpression, args)
    }

    private fun convertTypeArgument(
        arg: SignatureNode,
        annotationsByNode: Map<SignatureNode, List<Annotation>>,
    ): Expression {
        assert(arg.kind == TypeArgument) { "Unexpected kind ${arg.kind}, $TypeArgument expected" }

        val expression = if (arg.name == null) {
            converter.makeUnboundWildcard()
        } else {
            val argType = parseType(arg.children.single(), annotationsByNode)
            converter.makeWildcard(arg.name.single(), argType)
        }
        val annotations = annotationsByNode[arg].orEmpty()
        return if (annotations.isEmpty()) expression else converter.makeAnnotatedType(expression, annotations)
    }

    private fun parse(signature: String): SignatureNode {
        val parser = SignatureParserVisitor(converter.treeMaker::resolveQualifiedName)
        SignatureReader(signature).accept(parser)
        return parser.root
    }
}

private fun SignatureNode.findTypePathTarget(typePath: TypePath?): SignatureNode? {
    if (typePath == null) return this

    var current = this
    var innerClassIterator: Iterator<SignatureNode>? = null
    for (index in 0 until typePath.length) {
        val step = typePath.getStep(index)

        current = when (step) {
            TypePath.ARRAY_ELEMENT -> {
                if (current.kind != ArrayType) return null
                innerClassIterator = null

                current.children.single()
            }
            TypePath.INNER_TYPE if innerClassIterator != null -> {
                if (current.kind != InnerClass || !innerClassIterator.hasNext()) return null
                innerClassIterator.next()
            }
            TypePath.INNER_TYPE -> {
                if (current.kind != ClassType) return null
                innerClassIterator = current.children.filter { it.kind == InnerClass }.iterator()

                if (!innerClassIterator.hasNext()) return null
                innerClassIterator.next()
            }
            TypePath.WILDCARD_BOUND -> {
                if (current.kind != TypeArgument) return null
                innerClassIterator = null

                when (current.name) {
                    "+", "-" -> current.children.single()
                    else -> return null
                }
            }
            TypePath.TYPE_ARGUMENT -> {
                if (current.kind != ClassType && current.kind != InnerClass) return null
                innerClassIterator = null

                val argument = current.children.filter { it.kind == TypeArgument }
                    .getOrNull(typePath.getStepArgument(index))
                    ?: return null
                if (argument.name == "=") argument.children.single() else argument
            }

            else -> return null
        }
    }
    return current
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

private class SignatureParserVisitor(
    private val resolveQualifiedName: (String) -> KaptTreeMaker.ResolvedQualifiedName,
) : SignatureVisitor(Opcodes.API_VERSION) {
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
        val classInfo = resolveQualifiedName(name).classInfo
        // A generic signature may join nested and non-generic inner classes with '$' before the first type argument,
        // forming a "flat prefix". But for proper mapping of type annotation paths we need to know all inner classes,
        // so expose inner classes from that prefix as InnerClass nodes too, not only ones with explicit '.' suffixes.
        val namesInFlatPrefix = classInfo.indexOfLast { !it.isInner } + 1
        if (namesInFlatPrefix == classInfo.size) {
            push(ClassType, name = name)
            return
        }

        val packagePrefix = name.substringBeforeLast('/', missingDelimiterValue = "").let { if (it.isEmpty()) "" else "$it/" }
        val outerName = packagePrefix + classInfo.take(namesInFlatPrefix).joinToString("$") { it.name }
        push(ClassType, name = outerName)
        for ([innerName] in classInfo.drop(namesInFlatPrefix)) {
            push(InnerClass, name = innerName, parent = ClassType)
        }
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
