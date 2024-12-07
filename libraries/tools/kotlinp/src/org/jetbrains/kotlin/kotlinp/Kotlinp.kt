/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlin.metadata.*
import kotlin.contracts.ExperimentalContracts

abstract class Kotlinp(protected val settings: Settings) {
    fun renderAnnotation(annotation: KmAnnotation, printer: Printer): Unit = with(printer) {
        append(annotation.className)
        appendCollectionIfNotEmpty(annotation.arguments.entries, prefix = "(", postfix = ")") { (name, argument) ->
            append(name, " = ")
            renderAnnotationArgument(argument, printer)
        }
    }

    fun renderAnnotationArgument(argument: KmAnnotationArgument, printer: Printer): Unit = with(printer) {
        fun String.sanitize(quote: Char): String = buildString(length) {
            for (c in this@sanitize) {
                when (c) {
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    quote -> append("\\").append(quote)
                    else -> append(if (c.isISOControl()) "\\u%04x".format(c.code) else c)
                }
            }
        }

        when (argument) {
            is KmAnnotationArgument.ByteValue -> append("${argument.value}.toByte()")
            is KmAnnotationArgument.CharValue -> append("'${argument.value.toString().sanitize(quote = '\'')}'")
            is KmAnnotationArgument.ShortValue -> append("${argument.value}.toShort()")
            is KmAnnotationArgument.IntValue -> append(argument.value)
            is KmAnnotationArgument.LongValue -> append("${argument.value}L")
            is KmAnnotationArgument.FloatValue -> append("${argument.value}f")
            is KmAnnotationArgument.DoubleValue -> append(argument.value)
            is KmAnnotationArgument.UByteValue -> append("${argument.value}.toUByte()")
            is KmAnnotationArgument.UShortValue -> append("${argument.value}.toUShort()")
            is KmAnnotationArgument.UIntValue -> append("${argument.value}u")
            is KmAnnotationArgument.ULongValue -> append("${argument.value}uL")
            is KmAnnotationArgument.BooleanValue -> append(argument.value.toString())
            is KmAnnotationArgument.StringValue -> append("\"${argument.value.sanitize(quote = '"')}\"")
            is KmAnnotationArgument.KClassValue -> append("${argument.className}::class")
            is KmAnnotationArgument.ArrayKClassValue -> {
                repeat(argument.arrayDimensionCount) { append("kotlin/Array<") }
                append(argument.className, "::class")
                repeat(argument.arrayDimensionCount) { append(">") }
            }
            is KmAnnotationArgument.EnumValue -> append(argument.enumClassName, '.', argument.enumEntryName)
            is KmAnnotationArgument.AnnotationValue -> argument.annotation.let { annotation ->
                append(annotation.className)
                appendCollection(annotation.arguments.entries, prefix = "(", postfix = ")") { (name, argument) ->
                    append(name, " = ")
                    renderAnnotationArgument(argument, printer)
                }
            }
            is KmAnnotationArgument.ArrayValue -> appendCollection(argument.elements, prefix = "[", postfix = "]") { argument ->
                renderAnnotationArgument(argument, printer)
            }
        }
    }

    protected fun Printer.appendAnnotations(hasAnnotations: Boolean?, annotations: List<KmAnnotation>, onePerLine: Boolean = true) {
        if (hasAnnotations != false) {
            annotations.forEach { annotation ->
                append("@")
                renderAnnotation(annotation, this)
                if (onePerLine) appendLine() else append(" ")
            }
        }
    }

    @OptIn(ExperimentalContextReceivers::class)
    fun renderClass(clazz: KmClass, printer: Printer): Unit = with(printer) {
        appendOrigin(clazz)
        appendVersionRequirements(clazz.versionRequirements)
        appendSignatures(clazz)
        appendAnnotations(clazz.hasAnnotations, getAnnotations(clazz))
        appendContextReceiverTypes(clazz.contextReceiverTypes)
        append(VISIBILITY_MAP[clazz.visibility])
        append(MODALITY_MAP[clazz.modality])
        appendFlags(
            clazz.isInner to "inner",
            clazz.isData to "data",
            clazz.isExternal to "external",
            clazz.isExpect to "expect",
            clazz.isValue to "value",
            clazz.isFunInterface to "fun",
        )
        append(CLASS_KIND_MAP[clazz.kind])
        append(clazz.name)
        appendTypeParameters(clazz.typeParameters)
        appendCollectionIfNotEmpty(clazz.supertypes, prefix = " : ") { appendType(it) }
        appendLine(" {")
        withIndent {
            clazz.constructors.sortIfNeeded(::sortConstructors).forEach { renderConstructor(it, printer) }
            appendDeclarationContainerMembers(clazz)
            clazz.companionObject?.let {
                appendLine()
                appendCommentedLine("companion object: $it")
            }
            clazz.nestedClasses.forEach {
                appendLine()
                appendCommentedLine("nested class: $it")
            }
            appendEnumEntries(clazz)
            clazz.sealedSubclasses.sortIfNeeded { it }.forEach {
                appendLine()
                appendCommentedLine("sealed subclass: $it")
            }
            clazz.inlineClassUnderlyingPropertyName?.let {
                appendLine()
                appendCommentedLine("underlying property: $it")
            }
            clazz.inlineClassUnderlyingType?.let {
                appendLine()
                commented { append("underlying type: ").appendType(it).appendLine() }
            }
            appendCustomAttributes(clazz)
            if (clazz.hasEnumEntries) {
                appendLine()
                appendCommentedLine("has Enum.entries")
            }
        }
        appendLine("}")
    }

    private fun Printer.appendDeclarationContainerMembers(container: KmDeclarationContainer) {
        container.functions.sortIfNeeded(::sortFunctions).forEach { renderFunction(it, this) }
        container.properties.sortIfNeeded(::sortProperties).forEach { renderProperty(it, this) }
        container.typeAliases.sortIfNeeded { it.sortedBy(KmTypeAlias::name) }.forEach { renderTypeAlias(it, this) }
    }

    fun renderConstructor(constructor: KmConstructor, printer: Printer): Unit = with(printer) {
        appendLine()
        appendVersionRequirements(constructor.versionRequirements)
        appendSignatures(constructor)
        appendAnnotations(constructor.hasAnnotations, getAnnotations(constructor))
        renderConstructorModifiers(constructor, printer)
        append("constructor")
        appendValueParameters(constructor.valueParameters)
        appendLine()
    }

    fun renderConstructorModifiers(constructor: KmConstructor, printer: Printer): Unit = with(printer) {
        append(VISIBILITY_MAP[constructor.visibility])
        appendFlags(
            constructor.isSecondary to "/* secondary */",
            constructor.hasNonStableParameterNames to "/* non-stable parameter names */"
        )
    }

    @OptIn(ExperimentalContextReceivers::class, ExperimentalContracts::class)
    fun renderFunction(function: KmFunction, printer: Printer): Unit = with(printer) {
        appendLine()
        appendOrigin(function)
        appendVersionRequirements(function.versionRequirements)
        appendSignatures(function)
        appendAnnotations(function.hasAnnotations, getAnnotations(function))
        appendContextReceiverTypes(function.contextReceiverTypes)
        renderFunctionModifiers(function, printer)
        append("fun ")
        appendTypeParameters(function.typeParameters, postfix = " ")
        appendReceiverParameterType(function.receiverParameterType)
        append(function.name)
        appendValueParameters(function.valueParameters)
        append(": ").appendType(function.returnType)
        appendLine()
        function.contract?.let {
            withIndent {
                renderContract(it, printer)
                appendLine()
            }
        }
    }

    fun renderFunctionModifiers(function: KmFunction, printer: Printer): Unit = with(printer) {
        append(VISIBILITY_MAP[function.visibility])
        append(MODALITY_MAP[function.modality])
        append(MEMBER_KIND_MAP[function.kind])
        appendFlags(
            function.isOperator to "operator",
            function.isInfix to "infix",
            function.isInline to "inline",
            function.isTailrec to "tailrec",
            function.isExternal to "external",
            function.isSuspend to "suspend",
            function.isExpect to "expect",
            function.hasNonStableParameterNames to "/* non-stable parameter names */"
        )
    }

    @OptIn(ExperimentalContracts::class)
    fun renderContract(contract: KmContract, printer: Printer): Unit = with(printer) {
        appendLine("contract {")
        withIndent {
            contract.effects.forEach { effect ->
                renderEffect(effect, printer)
                appendLine()
            }
        }
        append("}")
    }

    @OptIn(ExperimentalContracts::class)
    private fun renderEffect(effect: KmEffect, printer: Printer) {
        // If there are several arguments, only the first is taken, see ContractDeserializerImpl.deserializeSimpleEffect
        fun Printer.appendMeaningfulConstructorArgument(effect: KmEffect): Printer {
            effect.constructorArguments.firstOrNull()?.let { append(printEffectExpression(it)) }
            return this
        }

        when (effect.type) {
            KmEffectType.RETURNS_CONSTANT -> {
                printer.append("returns(").appendMeaningfulConstructorArgument(effect).append(")")
            }

            KmEffectType.CALLS -> {
                printer.append("callsInPlace(").appendMeaningfulConstructorArgument(effect)
                effect.invocationKind?.let {
                    printer.append(", InvocationKind.${it.name}")
                }
                printer.append(")")
            }

            KmEffectType.RETURNS_NOT_NULL -> {
                printer.append("returnsNotNull()")
            }
        }
        effect.conclusion?.let {
            printer.append(" implies (", printEffectExpression(it), ")")
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun printEffectExpression(effectExpression: KmEffectExpression): String {
        val andArguments = effectExpression.andArguments.map(::printEffectExpression)
        val orArguments = effectExpression.orArguments.map(::printEffectExpression)

        fun wrapIfNeeded(s: String): String =
            // A simple heuristic to avoid wrapping into unnecessary parentheses
            if ('&' in s || '|' in s) "($s)" else s

        return printString {
            append(
                effectExpression.constantValue?.let { it.value.toString() }
                    ?: effectExpression.parameterIndex?.let { "p#$it" }
                    ?: ""
            )

            effectExpression.isInstanceType?.let { isInstanceType ->
                append(" ")
                if (effectExpression.isNegated) append("!")
                append("is ").appendType(isInstanceType)
            }

            if (effectExpression.isNullCheckPredicate) {
                append(if (effectExpression.isNegated) " != " else " == ")
                append("null")
            }

            if (orArguments.isEmpty()) {
                for (andArgument in andArguments) {
                    if (!isEmpty) append(" && ")
                    append(wrapIfNeeded(andArgument))
                }
            }
            if (andArguments.isEmpty()) {
                for (orArgument in orArguments) {
                    if (!isEmpty) append(" || ")
                    append(wrapIfNeeded(orArgument))
                }
            }
        }
    }

    fun renderPackage(pkg: KmPackage, printer: Printer, appendPackageAttributes: () -> Unit = {}): Unit = with(printer) {
        appendLine("package {")
        withIndent {
            appendPackageAttributes()
            appendDeclarationContainerMembers(pkg)
            appendCustomAttributes(pkg)
        }
        appendLine("}")
    }

    @OptIn(ExperimentalContextReceivers::class)
    fun renderProperty(property: KmProperty, printer: Printer): Unit = with(printer) {
        appendLine()
        appendVersionRequirements(property.versionRequirements)
        appendSignatures(property)
        appendCustomAttributes(property)
        appendAnnotations(property.hasAnnotations, getAnnotations(property))
        appendContextReceiverTypes(property.contextReceiverTypes)
        renderPropertyModifiers(property, printer)
        append(if (property.isVar) "var " else "val ")
        appendTypeParameters(property.typeParameters, postfix = " ")
        appendReceiverParameterType(property.receiverParameterType)
        append(property.name)
        append(": ").appendType(property.returnType)
        if (property.hasConstant) {
            append(" /* = ").appendCompileTimeConstant(property).append(" */")
        }
        appendLine()
        withIndent {
            appendGetterSignatures(property)
            appendAnnotations(property.getter.hasAnnotations, getGetterAnnotations(property))
            renderPropertyAccessorModifiers(property.getter, printer)
            appendLine("get")
            property.setter?.let { setter ->
                appendSetterSignatures(property)
                appendAnnotations(setter.hasAnnotations, getSetterAnnotations(property))
                renderPropertyAccessorModifiers(setter, printer)
                append("set")
                property.setterParameter?.let {
                    appendValueParameters(listOf(it))
                }
                appendLine()
            }
        }
    }

    fun renderPropertyModifiers(property: KmProperty, printer: Printer): Unit = with(printer) {
        append(VISIBILITY_MAP[property.visibility])
        append(MODALITY_MAP[property.modality])
        append(MEMBER_KIND_MAP[property.kind])
        appendFlags(
            property.isConst to "const",
            property.isLateinit to "lateinit",
            property.isExternal to "external",
            property.isDelegated to "/* delegated */",
            property.isExpect to "expect"
        )
    }

    fun renderPropertyAccessorModifiers(accessorAttributes: KmPropertyAccessorAttributes, printer: Printer): Unit = with(printer) {
        append(VISIBILITY_MAP[accessorAttributes.visibility])
        append(MODALITY_MAP[accessorAttributes.modality])
        appendFlags(
            accessorAttributes.isNotDefault to "/* non-default */",
            accessorAttributes.isExternal to "external",
            accessorAttributes.isInline to "inline"
        )
    }

    fun renderTypeAlias(typeAlias: KmTypeAlias, printer: Printer): Unit = with(printer) {
        appendLine()
        appendVersionRequirements(typeAlias.versionRequirements)
        appendSignatures(typeAlias)
        appendAnnotations(typeAlias.hasAnnotations, typeAlias.annotations)
        append(VISIBILITY_MAP[typeAlias.visibility], "typealias ", typeAlias.name)
        appendTypeParameters(typeAlias.typeParameters)
        append(" = ").appendType(typeAlias.underlyingType)
        append(" /* = ").appendType(typeAlias.expandedType).append(" */")
        appendLine()
    }

    fun renderTypeParameter(typeParameter: KmTypeParameter, printer: Printer): Unit = with(printer) {
        appendFlags(typeParameter.isReified to "reified")
        appendAnnotations(hasAnnotations = null, getAnnotations(typeParameter), onePerLine = false)
        if (typeParameter.variance != KmVariance.INVARIANT) {
            append(typeParameter.variance.name.lowercase()).append(" ")
        }
        append("T#${typeParameter.id}")
        if (settings.isVerbose) {
            append(" /* ${typeParameter.name} */")
        }
        appendCollectionIfNotEmpty(typeParameter.upperBounds, separator = " & ", prefix = " : ") { appendType(it) }
    }

    private fun Printer.appendTypeParameters(typeParameters: List<KmTypeParameter>, postfix: String = "") {
        appendCollectionIfNotEmpty(typeParameters, prefix = "<", postfix = ">$postfix") { renderTypeParameter(it, this) }
    }

    fun renderType(type: KmType, printer: Printer) {
        val classifier = when (val cls = type.classifier) {
            is KmClassifier.Class -> cls.name
            is KmClassifier.TypeParameter -> "T#${cls.id}"
            is KmClassifier.TypeAlias -> "${cls.name}$TYPE_ALIAS_MARKER"
        }

        val outerType = type.outerType
        val abbreviatedType = type.abbreviatedType
        val platformTypeUpperBound = type.flexibleTypeUpperBound?.let { renderFlexibleTypeUpperBound(it) }

        printer += printString {
            appendAnnotations(hasAnnotations = null, getAnnotations(type), onePerLine = false)
            appendFlags(
                isRaw(type) to "/* raw */",
                type.isSuspend to "suspend"
            )
            if (outerType != null) {
                appendType(outerType).append(".").append(classifier.substringAfterLast('.'))
            } else {
                append(classifier)
            }

            appendCollectionIfNotEmpty(type.arguments, prefix = "<", postfix = ">") { argument ->
                if (argument == KmTypeProjection.STAR) {
                    append("*")
                } else {
                    val (variance, argumentType) = argument
                    if (variance == null || argumentType == null)
                        throw IllegalArgumentException("Variance and type must be set for non-star type projection")

                    if (variance != KmVariance.INVARIANT) {
                        append(variance.name.lowercase()).append(" ")
                    }
                    appendType(argumentType)
                }
            }

            if (type.isNullable) append("?")
            if (type.isDefinitelyNonNull) append(" & Any")
            if (abbreviatedType != null) append(" /* = ").appendType(abbreviatedType).append(" */")

            if (platformTypeUpperBound == "$this?") {
                append("!")
            } else if (platformTypeUpperBound != null) {
                append("..").append(platformTypeUpperBound)
            }
        }
    }

    protected fun Printer.appendType(type: KmType): Printer {
        renderType(type, this)
        return this
    }

    private fun Printer.appendContextReceiverTypes(contextReceiverTypes: List<KmType>) {
        appendCollectionIfNotEmpty(contextReceiverTypes, prefix = "context(", postfix = ")\n") { appendType(it) }
    }

    private fun Printer.appendReceiverParameterType(receiverParameterType: KmType?) {
        receiverParameterType?.let {
            appendType(it).append(".")
        }
    }

    fun renderValueParameter(valueParameter: KmValueParameter, printer: Printer): Unit = with(printer) {
        appendAnnotations(valueParameter.hasAnnotations, getAnnotations(valueParameter), onePerLine = false)
        appendFlags(
            valueParameter.isCrossinline to "crossinline",
            valueParameter.isNoinline to "noinline"
        )
        val varargElementType = valueParameter.varargElementType
        if (varargElementType != null) {
            append("vararg ", valueParameter.name, ": ").appendType(varargElementType)
            append(" /* ").appendType(valueParameter.type).append(" */")
        } else {
            append(valueParameter.name, ": ").appendType(valueParameter.type)
        }
        if (valueParameter.declaresDefaultValue) {
            append(" /* = ... */")
        }
    }

    private fun Printer.appendValueParameters(valueParameters: List<KmValueParameter>) {
        appendCollection(valueParameters, prefix = "(", postfix = ")") { renderValueParameter(it, this) }
    }

    fun renderVersionRequirement(versionRequirement: KmVersionRequirement, printer: Printer) {
        val version = with(versionRequirement.version) { "$major.$minor.$patch" }
        val kind = when (versionRequirement.kind) {
            KmVersionRequirementVersionKind.LANGUAGE_VERSION -> "language version "
            KmVersionRequirementVersionKind.COMPILER_VERSION -> "compiler version "
            KmVersionRequirementVersionKind.API_VERSION -> "API version "
            KmVersionRequirementVersionKind.UNKNOWN -> "unknown requirement "
        }
        val remainder = listOfNotNull(
            "level=${versionRequirement.level}",
            versionRequirement.errorCode?.let { "errorCode=$it" },
            versionRequirement.message?.let { "message=\"$it\"" }
        ).joinToString(prefix = " (", postfix = ")")

        printer.append("requires ", kind, version, remainder)
    }

    private fun Printer.appendVersionRequirements(versionRequirements: List<KmVersionRequirement>) {
        versionRequirements.forEach { versionRequirement ->
            commented {
                renderVersionRequirement(versionRequirement, this)
                appendLine()
            }
        }
    }

    protected inline fun <T : Any> List<T>.sortIfNeeded(sorter: (List<T>) -> List<T>): List<T> =
        if (settings.sortDeclarations) sorter(this) else this

    companion object {
        private const val TYPE_ALIAS_MARKER = '^'

        internal val VISIBILITY_MAP = mapOf(
            Visibility.INTERNAL to "internal ",
            Visibility.PRIVATE to "private ",
            Visibility.PRIVATE_TO_THIS to "private ",
            Visibility.PROTECTED to "protected ",
            Visibility.PUBLIC to "public ",
            Visibility.LOCAL to "local "
        )

        internal val MODALITY_MAP = mapOf(
            Modality.FINAL to "final ",
            Modality.OPEN to "open ",
            Modality.ABSTRACT to "abstract ",
            Modality.SEALED to "sealed "
        )

        internal val CLASS_KIND_MAP = mapOf(
            ClassKind.CLASS to "class ",
            ClassKind.INTERFACE to "interface ",
            ClassKind.ENUM_CLASS to "enum class ",
            ClassKind.ENUM_ENTRY to "enum entry ",
            ClassKind.ANNOTATION_CLASS to "annotation class ",
            ClassKind.OBJECT to "object ",
            ClassKind.COMPANION_OBJECT to "companion object "
        )

        internal val MEMBER_KIND_MAP = mapOf(
            MemberKind.DECLARATION to "",
            MemberKind.FAKE_OVERRIDE to "/* fake override */ ",
            MemberKind.DELEGATION to "/* delegation */ ",
            MemberKind.SYNTHESIZED to "/* synthesized */ ",
        )
    }

    protected open fun getAnnotations(clazz: KmClass): List<KmAnnotation> = emptyList()
    protected open fun getAnnotations(constructor: KmConstructor): List<KmAnnotation> = emptyList()
    protected open fun getAnnotations(function: KmFunction): List<KmAnnotation> = emptyList()
    protected open fun getAnnotations(property: KmProperty): List<KmAnnotation> = emptyList()
    protected open fun getGetterAnnotations(property: KmProperty): List<KmAnnotation> = emptyList()
    protected open fun getSetterAnnotations(property: KmProperty): List<KmAnnotation> = emptyList()
    protected abstract fun getAnnotations(typeParameter: KmTypeParameter): List<KmAnnotation>
    protected abstract fun getAnnotations(type: KmType): List<KmAnnotation>
    protected open fun getAnnotations(valueParameter: KmValueParameter): List<KmAnnotation> = emptyList()

    protected open fun sortConstructors(constructors: List<KmConstructor>): List<KmConstructor> = constructors
    protected open fun sortFunctions(functions: List<KmFunction>): List<KmFunction> = functions
    protected open fun sortProperties(properties: List<KmProperty>): List<KmProperty> = properties

    protected open fun Printer.appendSignatures(clazz: KmClass) = Unit
    protected open fun Printer.appendSignatures(constructor: KmConstructor) = Unit
    protected open fun Printer.appendSignatures(function: KmFunction) = Unit
    protected open fun Printer.appendSignatures(property: KmProperty) = Unit
    protected open fun Printer.appendGetterSignatures(property: KmProperty) = Unit
    protected open fun Printer.appendSetterSignatures(property: KmProperty) = Unit
    protected open fun Printer.appendSignatures(typeAlias: KmTypeAlias) = Unit

    protected open fun Printer.appendOrigin(clazz: KmClass) = Unit
    protected open fun Printer.appendOrigin(function: KmFunction) = Unit

    protected abstract fun Printer.appendEnumEntries(clazz: KmClass)

    protected open fun Printer.appendCustomAttributes(clazz: KmClass) = Unit
    protected open fun Printer.appendCustomAttributes(pkg: KmPackage) = Unit
    protected open fun Printer.appendCustomAttributes(property: KmProperty) = Unit

    protected abstract fun Printer.appendCompileTimeConstant(property: KmProperty): Printer

    protected open fun isRaw(type: KmType): Boolean = false
    protected open fun renderFlexibleTypeUpperBound(flexibleTypeUpperBound: KmFlexibleTypeUpperBound): String? = null
}
