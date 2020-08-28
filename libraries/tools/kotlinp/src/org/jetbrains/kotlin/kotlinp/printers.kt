/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*

private object SpecialCharacters {
    const val TYPE_ALIAS_MARKER = '^'
}

private fun visitFunction(settings: KotlinpSettings, sb: StringBuilder, flags: Flags, name: String): KmFunctionVisitor =
    object : KmFunctionVisitor() {
        val typeParams = mutableListOf<String>()
        val params = mutableListOf<String>()
        var receiverParameterType: String? = null
        var returnType: String? = null
        val versionRequirements = mutableListOf<String>()
        var jvmSignature: JvmMemberSignature? = null
        var lambdaClassOriginName: String? = null
        var contract: String? = null

        override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
            printType(flags) { receiverParameterType = it }

        override fun visitTypeParameter(
            flags: Flags, name: String, id: Int, variance: KmVariance
        ): KmTypeParameterVisitor? =
            printTypeParameter(settings, flags, name, id, variance) { typeParams.add(it) }

        override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { params.add(it) }

        override fun visitReturnType(flags: Flags): KmTypeVisitor? =
            printType(flags) { returnType = it }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirements.add(it) }

        override fun visitContract(): KmContractVisitor? =
            printContract { contract = it }

        override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
            if (type != JvmFunctionExtensionVisitor.TYPE) return null
            return object : JvmFunctionExtensionVisitor() {
                override fun visit(signature: JvmMethodSignature?) {
                    jvmSignature = signature
                }

                override fun visitLambdaClassOriginName(internalName: String) {
                    lambdaClassOriginName = internalName
                }
            }
        }

        override fun visitEnd() {
            sb.appendLine()
            if (lambdaClassOriginName != null) {
                sb.appendLine("  // lambda class origin: $lambdaClassOriginName")
            }
            for (versionRequirement in versionRequirements) {
                sb.appendLine("  // $versionRequirement")
            }
            if (jvmSignature != null) {
                sb.appendLine("  // signature: $jvmSignature")
            }
            sb.append("  ")
            sb.appendFlags(flags, FUNCTION_FLAGS_MAP)
            sb.append("fun ")
            if (typeParams.isNotEmpty()) {
                typeParams.joinTo(sb, prefix = "<", postfix = ">")
                sb.append(" ")
            }
            if (receiverParameterType != null) {
                sb.append(receiverParameterType).append(".")
            }
            sb.append(name)
            params.joinTo(sb, prefix = "(", postfix = ")")
            if (returnType != null) {
                sb.append(": ").append(returnType)
            }
            sb.appendLine()
            if (contract != null) {
                sb.appendLine("    $contract")
            }
        }
    }

private fun visitProperty(
    settings: KotlinpSettings, sb: StringBuilder, flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
): KmPropertyVisitor =
    object : KmPropertyVisitor() {
        val typeParams = mutableListOf<String>()
        var receiverParameterType: String? = null
        var returnType: String? = null
        var setterParameter: String? = null
        val versionRequirements = mutableListOf<String>()
        var jvmFieldSignature: JvmMemberSignature? = null
        var jvmGetterSignature: JvmMemberSignature? = null
        var jvmSetterSignature: JvmMemberSignature? = null
        var jvmSyntheticMethodForAnnotationsSignature: JvmMemberSignature? = null
        var isMovedFromInterfaceCompanion: Boolean = false

        override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
            printType(flags) { receiverParameterType = it }

        override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
            printTypeParameter(settings, flags, name, id, variance) { typeParams.add(it) }

        override fun visitSetterParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { setterParameter = it }

        override fun visitReturnType(flags: Flags): KmTypeVisitor? =
            printType(flags) { returnType = it }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirements.add(it) }

        override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
            if (type != JvmPropertyExtensionVisitor.TYPE) return null
            return object : JvmPropertyExtensionVisitor() {
                override fun visit(
                    jvmFlags: Flags,
                    fieldSignature: JvmFieldSignature?,
                    getterSignature: JvmMethodSignature?,
                    setterSignature: JvmMethodSignature?
                ) {
                    isMovedFromInterfaceCompanion = JvmFlag.Property.IS_MOVED_FROM_INTERFACE_COMPANION(jvmFlags)
                    jvmFieldSignature = fieldSignature
                    jvmGetterSignature = getterSignature
                    jvmSetterSignature = setterSignature
                }

                override fun visitSyntheticMethodForAnnotations(signature: JvmMethodSignature?) {
                    jvmSyntheticMethodForAnnotationsSignature = signature
                }
            }
        }

        override fun visitEnd() {
            sb.appendLine()
            for (versionRequirement in versionRequirements) {
                sb.appendLine("  // $versionRequirement")
            }
            if (jvmFieldSignature != null) {
                sb.appendLine("  // field: $jvmFieldSignature")
            }
            if (jvmGetterSignature != null) {
                sb.appendLine("  // getter: $jvmGetterSignature")
            }
            if (jvmSetterSignature != null) {
                sb.appendLine("  // setter: $jvmSetterSignature")
            }
            if (jvmSyntheticMethodForAnnotationsSignature != null) {
                sb.appendLine("  // synthetic method for annotations: $jvmSyntheticMethodForAnnotationsSignature")
            }
            if (isMovedFromInterfaceCompanion) {
                sb.appendLine("  // is moved from interface companion")
            }
            sb.append("  ")
            sb.appendFlags(flags, PROPERTY_FLAGS_MAP)
            sb.append(if (Flag.Property.IS_VAR(flags)) "var " else "val ")
            if (typeParams.isNotEmpty()) {
                typeParams.joinTo(sb, prefix = "<", postfix = ">")
                sb.append(" ")
            }
            if (receiverParameterType != null) {
                sb.append(receiverParameterType).append(".")
            }
            sb.append(name)
            if (returnType != null) {
                sb.append(": ").append(returnType)
            }
            if (Flag.Property.HAS_CONSTANT(flags)) {
                sb.append(" /* = ... */")
            }
            sb.appendLine()
            if (Flag.Property.HAS_GETTER(flags)) {
                sb.append("    ")
                sb.appendFlags(getterFlags, PROPERTY_ACCESSOR_FLAGS_MAP)
                sb.appendLine("get")
            }
            if (Flag.Property.HAS_SETTER(flags)) {
                sb.append("    ")
                sb.appendFlags(setterFlags, PROPERTY_ACCESSOR_FLAGS_MAP)
                sb.append("set")
                if (setterParameter != null) {
                    sb.append("(").append(setterParameter).append(")")
                }
                sb.appendLine()
            }
        }
    }

private fun visitConstructor(sb: StringBuilder, flags: Flags): KmConstructorVisitor =
    object : KmConstructorVisitor() {
        val params = mutableListOf<String>()
        val versionRequirements = mutableListOf<String>()
        var jvmSignature: JvmMemberSignature? = null

        override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { params.add(it) }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirements.add(it) }

        override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
            if (type != JvmConstructorExtensionVisitor.TYPE) return null
            return object : JvmConstructorExtensionVisitor() {
                override fun visit(signature: JvmMethodSignature?) {
                    jvmSignature = signature
                }
            }
        }

        override fun visitEnd() {
            sb.appendLine()
            for (versionRequirement in versionRequirements) {
                sb.appendLine("  // $versionRequirement")
            }
            if (jvmSignature != null) {
                sb.appendLine("  // signature: $jvmSignature")
            }
            sb.append("  ")
            sb.appendFlags(flags, CONSTRUCTOR_FLAGS_MAP)
            sb.append("constructor(")
            params.joinTo(sb)
            sb.appendLine(")")
        }
    }

private fun visitTypeAlias(settings: KotlinpSettings, sb: StringBuilder, flags: Flags, name: String): KmTypeAliasVisitor =
    object : KmTypeAliasVisitor() {
        val annotations = mutableListOf<KmAnnotation>()
        val typeParams = mutableListOf<String>()
        var underlyingType: String? = null
        var expandedType: String? = null
        val versionRequirements = mutableListOf<String>()

        override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
            printTypeParameter(settings, flags, name, id, variance) { typeParams.add(it) }

        override fun visitUnderlyingType(flags: Flags): KmTypeVisitor? =
            printType(flags) { underlyingType = it }

        override fun visitExpandedType(flags: Flags): KmTypeVisitor? =
            printType(flags) { expandedType = it }

        override fun visitAnnotation(annotation: KmAnnotation) {
            annotations += annotation
        }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirements.add(it) }

        override fun visitEnd() {
            sb.appendLine()
            for (versionRequirement in versionRequirements) {
                sb.appendLine("  // $versionRequirement")
            }
            for (annotation in annotations) {
                sb.append("  ").append("@").append(renderAnnotation(annotation)).appendLine()
            }
            sb.append("  ")
            sb.appendFlags(flags, VISIBILITY_FLAGS_MAP)
            sb.append("typealias ").append(name)
            if (typeParams.isNotEmpty()) {
                typeParams.joinTo(sb, prefix = "<", postfix = ">")
            }
            if (underlyingType != null) {
                sb.append(" = ").append(underlyingType)
            }
            if (expandedType != null) {
                sb.append(" /* = ").append(expandedType).append(" */")
            }
            sb.appendLine()
        }
    }

private fun printType(flags: Flags, output: (String) -> Unit): KmTypeVisitor =
    object : KmTypeVisitor() {
        var classifier: String? = null
        val arguments = mutableListOf<String>()
        var abbreviatedType: String? = null
        var outerType: String? = null
        var platformTypeUpperBound: String? = null
        var jvmIsRaw: Boolean? = null
        var jvmAnnotations = mutableListOf<KmAnnotation>()

        override fun visitClass(name: ClassName) {
            classifier = name
        }

        override fun visitTypeParameter(id: Int) {
            classifier = "T#$id"
        }

        override fun visitTypeAlias(name: ClassName) {
            classifier = "$name${SpecialCharacters.TYPE_ALIAS_MARKER}"
        }

        override fun visitAbbreviatedType(flags: Flags): KmTypeVisitor? =
            printType(flags) { abbreviatedType = it }

        override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor? =
            printType(flags) { argumentTypeString ->
                arguments += buildString {
                    if (variance != KmVariance.INVARIANT) {
                        append(variance.name.toLowerCase()).append(" ")
                    }
                    append(argumentTypeString)
                }
            }

        override fun visitStarProjection() {
            arguments += "*"
        }

        override fun visitOuterType(flags: Flags): KmTypeVisitor? =
            printType(flags) { outerType = it }

        override fun visitFlexibleTypeUpperBound(flags: Flags, typeFlexibilityId: String?): KmTypeVisitor? =
            if (typeFlexibilityId == JvmTypeExtensionVisitor.PLATFORM_TYPE_ID) {
                printType(flags) { platformTypeUpperBound = it }
            } else null

        override fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? {
            if (type != JvmTypeExtensionVisitor.TYPE) return null
            return object : JvmTypeExtensionVisitor() {
                override fun visit(isRaw: Boolean) {
                    jvmIsRaw = isRaw
                }

                override fun visitAnnotation(annotation: KmAnnotation) {
                    jvmAnnotations.add(annotation)
                }
            }
        }

        override fun visitEnd() {
            output(buildString {
                for (annotation in jvmAnnotations) {
                    append("@").append(renderAnnotation(annotation)).append(" ")
                }
                if (jvmIsRaw == true) {
                    append("/* raw */ ")
                }
                appendFlags(flags, TYPE_FLAGS_MAP)
                if (outerType != null) {
                    append(outerType).append(".").append(classifier?.substringAfterLast('.'))
                } else {
                    append(classifier)
                }
                if (arguments.isNotEmpty()) {
                    arguments.joinTo(this, prefix = "<", postfix = ">")
                }
                if (Flag.Type.IS_NULLABLE(flags)) {
                    append("?")
                }
                if (abbreviatedType != null) {
                    append(" /* = ").append(abbreviatedType).append(" */")
                }

                if (platformTypeUpperBound == "$this?") {
                    append("!")
                } else if (platformTypeUpperBound != null) {
                    append("..").append(platformTypeUpperBound)
                }
            })
        }
    }

private fun printTypeParameter(
    settings: KotlinpSettings, flags: Flags, name: String, id: Int, variance: KmVariance, output: (String) -> Unit
): KmTypeParameterVisitor =
    object : KmTypeParameterVisitor() {
        val bounds = mutableListOf<String>()
        val jvmAnnotations = mutableListOf<KmAnnotation>()

        override fun visitUpperBound(flags: Flags): KmTypeVisitor? =
            printType(flags) { bounds += it }

        override fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? {
            if (type != JvmTypeParameterExtensionVisitor.TYPE) return null
            return object : JvmTypeParameterExtensionVisitor() {
                override fun visitAnnotation(annotation: KmAnnotation) {
                    jvmAnnotations.add(annotation)
                }
            }
        }

        override fun visitEnd() {
            output(buildString {
                appendFlags(flags, TYPE_PARAMETER_FLAGS_MAP)
                for (annotation in jvmAnnotations) {
                    append("@").append(renderAnnotation(annotation)).append(" ")
                }
                if (variance != KmVariance.INVARIANT) {
                    append(variance.name.toLowerCase()).append(" ")
                }
                append("T#$id")
                if (settings.isVerbose) {
                    append(" /* $name */")
                }
                if (bounds.isNotEmpty()) {
                    bounds.joinTo(this, separator = " & ", prefix = " : ")
                }
            })
        }
    }

private fun printValueParameter(flags: Flags, name: String, output: (String) -> Unit): KmValueParameterVisitor =
    object : KmValueParameterVisitor() {
        var varargElementType: String? = null
        var type: String? = null

        override fun visitType(flags: Flags): KmTypeVisitor? =
            printType(flags) { type = it }

        override fun visitVarargElementType(flags: Flags): KmTypeVisitor? =
            printType(flags) { varargElementType = it }

        override fun visitEnd() {
            output(buildString {
                appendFlags(flags, VALUE_PARAMETER_FLAGS_MAP)
                if (varargElementType != null) {
                    append("vararg ").append(name).append(": ").append(varargElementType).append(" /* ").append(type).append(" */")
                } else {
                    append(name).append(": ").append(type)
                }
                if (Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)) {
                    append(" /* = ... */")
                }
            })
        }
    }

private fun renderAnnotation(annotation: KmAnnotation): String =
    annotation.className + if (annotation.arguments.isEmpty()) "" else
        annotation.arguments.entries.joinToString(prefix = "(", postfix = ")") { (name, argument) ->
            "$name = ${renderAnnotationArgument(argument)}"
        }

@OptIn(ExperimentalUnsignedTypes::class)
private fun renderAnnotationArgument(arg: KmAnnotationArgument<*>): String =
    when (arg) {
        is KmAnnotationArgument.ByteValue -> arg.value.toString() + ".toByte()"
        is KmAnnotationArgument.CharValue -> "'${arg.value.toString().sanitize(quote = '\'')}'"
        is KmAnnotationArgument.ShortValue -> arg.value.toString() + ".toShort()"
        is KmAnnotationArgument.IntValue -> arg.value.toString()
        is KmAnnotationArgument.LongValue -> arg.value.toString() + "L"
        is KmAnnotationArgument.FloatValue -> arg.value.toString() + "f"
        is KmAnnotationArgument.DoubleValue -> arg.value.toString()
        is KmAnnotationArgument.UByteValue -> arg.value.toUByte().toString() + ".toUByte()"
        is KmAnnotationArgument.UShortValue -> arg.value.toUShort().toString() + ".toUShort()"
        is KmAnnotationArgument.UIntValue -> arg.value.toUInt().toString() + "u"
        is KmAnnotationArgument.ULongValue -> arg.value.toULong().toString() + "uL"
        is KmAnnotationArgument.BooleanValue -> arg.value.toString()
        is KmAnnotationArgument.StringValue -> "\"${arg.value.sanitize(quote = '"')}\""
        is KmAnnotationArgument.KClassValue -> "${arg.value}::class"
        is KmAnnotationArgument.EnumValue -> arg.value
        is KmAnnotationArgument.AnnotationValue -> arg.value.let { annotation ->
            val args = annotation.arguments.entries.joinToString { (name, argument) ->
                "$name = ${renderAnnotationArgument(argument)}"
            }
            "${annotation.className}($args)"
        }
        is KmAnnotationArgument.ArrayValue -> arg.value.joinToString(prefix = "[", postfix = "]", transform = ::renderAnnotationArgument)
    }

private fun String.sanitize(quote: Char): String =
    buildString(length) {
        for (c in this@sanitize) {
            when (c) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                quote -> append("\\").append(quote)
                else -> append(if (c.isISOControl()) "\\u%04x".format(c.toInt()) else c)
            }
        }
    }

private fun printVersionRequirement(output: (String) -> Unit): KmVersionRequirementVisitor =
    object : KmVersionRequirementVisitor() {
        var kind: KmVersionRequirementVersionKind? = null
        var level: KmVersionRequirementLevel? = null
        var errorCode: Int? = null
        var message: String? = null
        var version: String? = null

        override fun visit(kind: KmVersionRequirementVersionKind, level: KmVersionRequirementLevel, errorCode: Int?, message: String?) {
            this.kind = kind
            this.level = level
            this.errorCode = errorCode
            this.message = message
        }

        override fun visitVersion(major: Int, minor: Int, patch: Int) {
            version = "$major.$minor.$patch"
        }

        override fun visitEnd() {
            output(buildString {
                append("requires ").append(
                    when (kind!!) {
                        KmVersionRequirementVersionKind.LANGUAGE_VERSION -> "language version"
                        KmVersionRequirementVersionKind.COMPILER_VERSION -> "compiler version"
                        KmVersionRequirementVersionKind.API_VERSION -> "API version"
                    }
                ).append(" ").append(version)

                listOfNotNull(
                    "level=$level",
                    errorCode?.let { "errorCode=$it" },
                    message?.let { "message=\"$it\"" }
                ).joinTo(this, prefix = " (", postfix = ")")
            })
        }
    }

private fun StringBuilder.appendFlags(flags: Flags, map: Map<Flag, String>) {
    for ((modifier, string) in map) {
        if (modifier(flags)) {
            append(string)
            if (string.isNotEmpty()) append(" ")
        }
    }
}

private fun StringBuilder.appendDeclarationContainerExtensions(
    settings: KotlinpSettings,
    localDelegatedProperties: List<StringBuilder>,
    moduleName: String?
) {
    for ((i, sb) in localDelegatedProperties.withIndex()) {
        appendLine()
        appendLine("  // local delegated property #$i")
        for (line in sb.lineSequence()) {
            if (line.isBlank()) continue
            // Comment all uncommented lines to not make it look like these properties are declared here
            appendLine(
                if (line.startsWith("  ") && !line.startsWith("  //")) line.replaceFirst("  ", "  // ")
                else line
            )
        }
    }

    if (settings.isVerbose && moduleName != null) {
        appendLine()
        appendLine("  // module name: $moduleName")
    }
}

private fun printContract(output: (String) -> Unit): KmContractVisitor =
    object : KmContractVisitor() {
        val effects = mutableListOf<String>()

        override fun visitEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?): KmEffectVisitor =
            printEffect(type, invocationKind) { effects.add(it) }

        override fun visitEnd() {
            output(buildString {
                appendLine("contract {")
                for (effect in effects) {
                    appendLine("      $effect")
                }
                append("    }")
            })
        }
    }

private fun printEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?, output: (String) -> Unit): KmEffectVisitor =
    object : KmEffectVisitor() {
        var argument: String? = null
        var conclusion: String? = null

        override fun visitConstructorArgument(): KmEffectExpressionVisitor =
            printEffectExpression {
                // If there are several arguments, only the first is taken, see ContractDeserializerImpl.deserializeSimpleEffect
                if (argument == null) {
                    argument = it
                }
            }

        override fun visitConclusionOfConditionalEffect(): KmEffectExpressionVisitor =
            printEffectExpression { conclusion = it }

        override fun visitEnd() {
            output(buildString {
                when (type) {
                    KmEffectType.RETURNS_CONSTANT -> {
                        append("returns(")
                        if (argument != null) {
                            append(argument)
                        }
                        append(")")
                    }
                    KmEffectType.CALLS -> {
                        append("callsInPlace($argument")
                        if (invocationKind != null) {
                            append(", InvocationKind.${invocationKind.name}")
                        }
                        append(")")
                    }
                    KmEffectType.RETURNS_NOT_NULL -> {
                        append("returnsNotNull()")
                    }
                }
                if (conclusion != null) {
                    append(" implies ($conclusion)")
                }
            })
        }
    }

private fun printEffectExpression(output: (String) -> Unit): KmEffectExpressionVisitor =
    object : KmEffectExpressionVisitor() {
        var flags: Flags = 0
        var parameterIndex: Int? = null
        var constantValue: List<Any?>? = null // Single-element list
        var isInstanceType: String? = null
        var andArguments = mutableListOf<String>()
        var orArguments = mutableListOf<String>()

        override fun visit(flags: Flags, parameterIndex: Int?) {
            this.flags = flags
            this.parameterIndex = parameterIndex
        }

        override fun visitConstantValue(value: Any?) {
            constantValue = listOf(value)
        }

        override fun visitIsInstanceType(flags: Flags): KmTypeVisitor =
            printType(flags) { isInstanceType = it }

        override fun visitAndArgument(): KmEffectExpressionVisitor =
            printEffectExpression { andArguments.add(it) }

        override fun visitOrArgument(): KmEffectExpressionVisitor =
            printEffectExpression { orArguments.add(it) }

        override fun visitEnd() {
            output(buildString {
                append(
                    when {
                        constantValue != null -> constantValue!!.single().toString()
                        parameterIndex != null -> "p#$parameterIndex"
                        else -> ""
                    }
                )
                if (isInstanceType != null) {
                    append(" ")
                    if (Flag.EffectExpression.IS_NEGATED(flags)) append("!")
                    append("is $isInstanceType")
                }
                if (Flag.EffectExpression.IS_NULL_CHECK_PREDICATE(flags)) {
                    append(if (Flag.EffectExpression.IS_NEGATED(flags)) " != " else " == ")
                    append("null")
                }

                if (orArguments.isEmpty()) {
                    for (andArgument in andArguments) {
                        if (!isEmpty()) append(" && ")
                        append(wrapIfNeeded(andArgument))
                    }
                }
                if (andArguments.isEmpty()) {
                    for (orArgument in orArguments) {
                        if (!isEmpty()) append(" || ")
                        append(wrapIfNeeded(orArgument))
                    }
                }
            })
        }

        private fun wrapIfNeeded(s: String): String =
            // A simple heuristic to avoid wrapping into unnecessary parentheses
            if ('&' in s || '|' in s) "($s)" else s
    }

interface AbstractPrinter<in T : KotlinClassMetadata> {
    fun print(klass: T): String
}

class ClassPrinter(private val settings: KotlinpSettings) : KmClassVisitor(), AbstractPrinter<KotlinClassMetadata.Class> {
    private val sb = StringBuilder()
    internal val result = StringBuilder()

    private var flags: Flags? = null
    private var name: ClassName? = null
    private val typeParams = mutableListOf<String>()
    private val supertypes = mutableListOf<String>()
    private val versionRequirements = mutableListOf<String>()
    private var anonymousObjectOriginName: String? = null

    override fun visit(flags: Flags, name: ClassName) {
        this.flags = flags
        this.name = name
    }

    override fun visitEnd() {
        if (anonymousObjectOriginName != null) {
            result.appendLine("// anonymous object origin: $anonymousObjectOriginName")
        }
        for (versionRequirement in versionRequirements) {
            result.appendLine("// $versionRequirement")
        }
        result.appendFlags(flags!!, CLASS_FLAGS_MAP)
        result.append(name)
        if (typeParams.isNotEmpty()) {
            typeParams.joinTo(result, prefix = "<", postfix = ">")
        }
        if (supertypes.isNotEmpty()) {
            result.append(" : ")
            supertypes.joinTo(result)
        }
        result.appendLine(" {")
        result.append(sb)
        result.appendLine("}")
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        printTypeParameter(settings, flags, name, id, variance) { typeParams.add(it) }

    override fun visitSupertype(flags: Flags): KmTypeVisitor? =
        printType(flags) { supertypes.add(it) }

    override fun visitConstructor(flags: Flags): KmConstructorVisitor? =
        visitConstructor(sb, flags)

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        visitFunction(settings, sb, flags, name)

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        visitProperty(settings, sb, flags, name, getterFlags, setterFlags)

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? =
        visitTypeAlias(settings, sb, flags, name)

    override fun visitCompanionObject(name: String) {
        sb.appendLine()
        sb.appendLine("  // companion object: $name")
    }

    override fun visitNestedClass(name: String) {
        sb.appendLine()
        sb.appendLine("  // nested class: $name")
    }

    override fun visitEnumEntry(name: String) {
        sb.appendLine()
        sb.appendLine("  $name,")
    }

    override fun visitSealedSubclass(name: ClassName) {
        sb.appendLine()
        sb.appendLine("  // sealed subclass: $name")
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        printVersionRequirement { versionRequirements.add(it) }

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? {
        if (type != JvmClassExtensionVisitor.TYPE) return null
        return object : JvmClassExtensionVisitor() {
            private val localDelegatedProperties = mutableListOf<StringBuilder>()
            private var moduleName: String? = null

            override fun visitAnonymousObjectOriginName(internalName: String) {
                anonymousObjectOriginName = internalName
            }

            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = visitProperty(
                settings, StringBuilder().also { localDelegatedProperties.add(it) }, flags, name, getterFlags, setterFlags
            )

            override fun visitModuleName(name: String) {
                moduleName = name
            }

            override fun visitEnd() {
                sb.appendDeclarationContainerExtensions(settings, localDelegatedProperties, moduleName)
            }
        }
    }

    override fun print(klass: KotlinClassMetadata.Class): String {
        klass.accept(this)
        return result.toString()
    }
}

abstract class PackagePrinter(private val settings: KotlinpSettings) : KmPackageVisitor() {
    internal val sb = StringBuilder().apply {
        appendLine("package {")
    }

    override fun visitEnd() {
        sb.appendLine("}")
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        visitFunction(settings, sb, flags, name)

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        visitProperty(settings, sb, flags, name, getterFlags, setterFlags)

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? =
        visitTypeAlias(settings, sb, flags, name)

    override fun visitExtensions(type: KmExtensionType): KmPackageExtensionVisitor? {
        if (type != JvmPackageExtensionVisitor.TYPE) return null
        return object : JvmPackageExtensionVisitor() {
            private val localDelegatedProperties = mutableListOf<StringBuilder>()
            private var moduleName: String? = null

            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = visitProperty(
                settings, StringBuilder().also { localDelegatedProperties.add(it) }, flags, name, getterFlags, setterFlags
            )

            override fun visitEnd() {
                sb.appendDeclarationContainerExtensions(settings, localDelegatedProperties, moduleName)
            }
        }
    }
}

class FileFacadePrinter(settings: KotlinpSettings) : PackagePrinter(settings), AbstractPrinter<KotlinClassMetadata.FileFacade> {
    override fun print(klass: KotlinClassMetadata.FileFacade): String {
        klass.accept(this)
        return sb.toString()
    }
}

class LambdaPrinter(private val settings: KotlinpSettings) : KmLambdaVisitor(), AbstractPrinter<KotlinClassMetadata.SyntheticClass> {
    private val sb = StringBuilder().apply {
        appendLine("lambda {")
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        visitFunction(settings, sb, flags, name)

    override fun visitEnd() {
        sb.appendLine("}")
    }

    override fun print(klass: KotlinClassMetadata.SyntheticClass): String {
        klass.accept(this)
        return sb.toString()
    }
}

class MultiFileClassPartPrinter(
    settings: KotlinpSettings
) : PackagePrinter(settings), AbstractPrinter<KotlinClassMetadata.MultiFileClassPart> {
    override fun print(klass: KotlinClassMetadata.MultiFileClassPart): String {
        sb.appendLine("  // facade: ${klass.facadeClassName}")
        klass.accept(this)
        return sb.toString()
    }
}

class MultiFileClassFacadePrinter : AbstractPrinter<KotlinClassMetadata.MultiFileClassFacade> {
    override fun print(klass: KotlinClassMetadata.MultiFileClassFacade): String =
        buildString {
            appendLine("multi-file class {")
            for (part in klass.partClassNames) {
                appendLine("  // $part")
            }
            appendLine("}")
        }
}

class ModuleFilePrinter(private val settings: KotlinpSettings) : KmModuleVisitor() {
    private val optionalAnnotations = mutableListOf<ClassPrinter>()

    private val sb = StringBuilder().apply {
        appendLine("module {")
    }

    override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        val presentableFqName = if (fqName.isEmpty()) "<root>" else fqName
        sb.appendLine("  package $presentableFqName {")
        for (fileFacade in fileFacades) {
            sb.appendLine("    $fileFacade")
        }
        for ((multiFileClassPart, facade) in multiFileClassParts) {
            sb.appendLine("    $multiFileClassPart ($facade)")
        }
        sb.appendLine("  }")
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        // TODO
    }

    override fun visitOptionalAnnotationClass(): KmClassVisitor =
        ClassPrinter(settings).also(optionalAnnotations::add)

    override fun visitEnd() {
        if (optionalAnnotations.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("  // Optional annotations")
            sb.appendLine()
            for (element in optionalAnnotations) {
                sb.appendLine("  " + element.result.toString().replace("\n", "\n  ").trimEnd())
            }
        }
        sb.appendLine("}")
    }

    fun print(metadata: KotlinModuleMetadata): String {
        metadata.accept(this)
        return sb.toString()
    }
}

private val VISIBILITY_FLAGS_MAP = mapOf(
    Flag.IS_INTERNAL to "internal",
    Flag.IS_PRIVATE to "private",
    Flag.IS_PRIVATE_TO_THIS to "private",
    Flag.IS_PROTECTED to "protected",
    Flag.IS_PUBLIC to "public",
    Flag.IS_LOCAL to "local"
)

private val COMMON_FLAGS_MAP = VISIBILITY_FLAGS_MAP + mapOf(
    Flag.IS_FINAL to "final",
    Flag.IS_OPEN to "open",
    Flag.IS_ABSTRACT to "abstract",
    Flag.IS_SEALED to "sealed"
)

private val CLASS_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flag.Class.IS_INNER to "inner",
    Flag.Class.IS_DATA to "data",
    Flag.Class.IS_EXTERNAL to "external",
    Flag.Class.IS_EXPECT to "expect",
    Flag.Class.IS_INLINE to "inline",
    Flag.Class.IS_FUN to "fun",

    Flag.Class.IS_CLASS to "class",
    Flag.Class.IS_INTERFACE to "interface",
    Flag.Class.IS_ENUM_CLASS to "enum class",
    Flag.Class.IS_ENUM_ENTRY to "enum entry",
    Flag.Class.IS_ANNOTATION_CLASS to "annotation class",
    Flag.Class.IS_OBJECT to "object",
    Flag.Class.IS_COMPANION_OBJECT to "companion object"
)

private val CONSTRUCTOR_FLAGS_MAP = VISIBILITY_FLAGS_MAP + mapOf(
    Flag.Constructor.IS_PRIMARY to "/* primary */",
    Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES to "/* non-stable parameter names */"
)

private val FUNCTION_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flag.Function.IS_DECLARATION to "",
    Flag.Function.IS_FAKE_OVERRIDE to "/* fake override */",
    Flag.Function.IS_DELEGATION to "/* delegation */",
    Flag.Function.IS_SYNTHESIZED to "/* synthesized */",

    Flag.Function.IS_OPERATOR to "operator",
    Flag.Function.IS_INFIX to "infix",
    Flag.Function.IS_INLINE to "inline",
    Flag.Function.IS_TAILREC to "tailrec",
    Flag.Function.IS_EXTERNAL to "external",
    Flag.Function.IS_SUSPEND to "suspend",
    Flag.Function.IS_EXPECT to "expect",

    Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES to "/* non-stable parameter names */"
)

private val PROPERTY_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flag.Property.IS_DECLARATION to "",
    Flag.Property.IS_FAKE_OVERRIDE to "/* fake override */",
    Flag.Property.IS_DELEGATION to "/* delegation */",
    Flag.Property.IS_SYNTHESIZED to "/* synthesized */",

    Flag.Property.IS_CONST to "const",
    Flag.Property.IS_LATEINIT to "lateinit",
    Flag.Property.IS_EXTERNAL to "external",
    Flag.Property.IS_DELEGATED to "/* delegated */",
    Flag.Property.IS_EXPECT to "expect"
)

private val PROPERTY_ACCESSOR_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flag.PropertyAccessor.IS_NOT_DEFAULT to "/* non-default */",
    Flag.PropertyAccessor.IS_EXTERNAL to "external",
    Flag.PropertyAccessor.IS_INLINE to "inline"
)

private val VALUE_PARAMETER_FLAGS_MAP = mapOf(
    Flag.ValueParameter.IS_CROSSINLINE to "crossinline",
    Flag.ValueParameter.IS_NOINLINE to "noinline"
)

private val TYPE_PARAMETER_FLAGS_MAP = mapOf(
    Flag.TypeParameter.IS_REIFIED to "reified"
)

private val TYPE_FLAGS_MAP = mapOf(
    Flag.Type.IS_SUSPEND to "suspend"
)
