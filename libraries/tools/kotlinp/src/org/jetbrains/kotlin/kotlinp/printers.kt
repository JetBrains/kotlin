/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.*
import kotlinx.metadata.jvm.*
import kotlin.contracts.ExperimentalContracts

private object SpecialCharacters {
    const val TYPE_ALIAS_MARKER = '^'
}

@OptIn(ExperimentalContextReceivers::class, ExperimentalContracts::class)
private fun visitFunction(
    function: KmFunction,
    settings: KotlinpSettings,
    sb: StringBuilder
) {
    sb.appendLine()
    function.lambdaClassOriginName?.let {
        sb.appendLine("  // lambda class origin: $it")
    }
    function.versionRequirements.map(::printVersionRequirement).forEach { versionRequirement ->
        sb.appendLine("  // $versionRequirement")
    }
    function.signature?.let {
        sb.appendLine("  // signature: $it")
    }

    if (function.contextReceiverTypes.isNotEmpty()) {
        sb.appendLine(function.contextReceiverTypes.joinToString(prefix = "  context(", postfix = ")", transform = ::printType))
    }
    sb.append("  ")
    sb.appendFunctionModifiers(function)
    sb.append("fun ")
    if (function.typeParameters.isNotEmpty()) {
        function.typeParameters.joinTo(sb, prefix = "<", postfix = ">", transform = { printTypeParameter(it, settings) })
        sb.append(" ")
    }
    function.receiverParameterType?.let {
        sb.append(printType(it)).append(".")
    }
    sb.append(function.name)
    function.valueParameters.joinTo(sb, prefix = "(", postfix = ")", transform = ::printValueParameter)
    sb.append(": ").append(printType(function.returnType))
    sb.appendLine()
    function.contract?.let {
        sb.appendLine("    ${printContract(it)}")
    }
}

@OptIn(ExperimentalContextReceivers::class)
private fun visitProperty(
    property: KmProperty,
    settings: KotlinpSettings,
    sb: StringBuilder
) {
    sb.appendLine()
    property.versionRequirements.map(::printVersionRequirement).forEach { versionRequirement ->
        sb.appendLine("  // $versionRequirement")
    }
    if (property.fieldSignature != null) {
        sb.appendLine("  // field: ${property.fieldSignature}")
    }
    if (property.getterSignature != null) {
        sb.appendLine("  // getter: ${property.getterSignature}")
    }
    if (property.setterSignature != null) {
        sb.appendLine("  // setter: ${property.setterSignature}")
    }
    if (property.syntheticMethodForAnnotations != null) {
        sb.appendLine("  // synthetic method for annotations: ${property.syntheticMethodForAnnotations}")
    }
    if (property.syntheticMethodForDelegate != null) {
        sb.appendLine("  // synthetic method for delegate: ${property.syntheticMethodForDelegate}")
    }
    if (property.isMovedFromInterfaceCompanion) {
        sb.appendLine("  // is moved from interface companion")
    }
    if (property.contextReceiverTypes.isNotEmpty()) {
        sb.appendLine(property.contextReceiverTypes.joinToString(prefix = "  context(", postfix = ")", transform = ::printType))
    }
    sb.append("  ")
    sb.appendPropertyModifiers(property)
    sb.append(if (property.isVar) "var " else "val ")
    if (property.typeParameters.isNotEmpty()) {
        property.typeParameters.joinTo(sb, prefix = "<", postfix = ">", transform = { printTypeParameter(it, settings) })
        sb.append(" ")
    }
    property.receiverParameterType?.let {
        sb.append(printType(it)).append(".")
    }
    sb.append(property.name)
    sb.append(": ").append(property.returnType.let(::printType))
    if (property.hasConstant) {
        sb.append(" /* = ... */")
    }
    sb.appendLine()
    sb.append("    ")
    sb.appendPropertyAccessorModifiers(property.getter)
    sb.appendLine("get")
    val setter = property.setter
    if (setter != null) {
        sb.append("    ")
        sb.appendPropertyAccessorModifiers(setter)
        sb.append("set")
        property.setterParameter?.let {
            sb.append("(").append(printValueParameter(it)).append(")")
        }
        sb.appendLine()
    }
}


private fun visitConstructor(constructor: KmConstructor, sb: StringBuilder) {
    sb.appendLine()
    constructor.versionRequirements.map(::printVersionRequirement).forEach { versionRequirement ->
        sb.appendLine("  // $versionRequirement")
    }
    if (constructor.signature != null) {
        sb.appendLine("  // signature: ${constructor.signature}")
    }
    sb.append("  ")
    sb.appendConstructorModifiers(constructor)
    sb.append("constructor(")
    constructor.valueParameters.joinTo(sb, transform = ::printValueParameter)
    sb.appendLine(")")
}

private fun visitTypeAlias(
    typeAlias: KmTypeAlias,
    settings: KotlinpSettings,
    sb: StringBuilder
) {
    sb.appendLine()
    typeAlias.versionRequirements.map(::printVersionRequirement).forEach { versionRequirement ->
        sb.appendLine("  // $versionRequirement")
    }
    typeAlias.annotations.forEach { annotation ->
        sb.append("  ").append("@").append(renderAnnotation(annotation)).appendLine()
    }
    sb.append("  ")
    sb.append(VISIBILITY_MAP[typeAlias.visibility])
    sb.append("typealias ").append(typeAlias.name)
    if (typeAlias.typeParameters.isNotEmpty()) {
        typeAlias.typeParameters.joinTo(sb, prefix = "<", postfix = ">") { printTypeParameter(it, settings) }
    }
    sb.append(" = ").append(typeAlias.underlyingType.let(::printType))
    sb.append(" /* = ").append(typeAlias.expandedType.let(::printType)).append(" */")
    sb.appendLine()
}

private fun printType(type: KmType): String {
    val classifier = when (val cls = type.classifier) {
        is KmClassifier.Class -> cls.name
        is KmClassifier.TypeParameter -> "T#${cls.id}"
        is KmClassifier.TypeAlias -> "${cls.name}${SpecialCharacters.TYPE_ALIAS_MARKER}"
    }

    val arguments = mutableListOf<String>()
    type.arguments.forEach { argument ->
        arguments += if (argument == KmTypeProjection.STAR) {
            "*"
        } else {
            val (variance, argumentType) = argument
            if (variance == null || argumentType == null)
                throw IllegalArgumentException("Variance and type must be set for non-star type projection")
            val argumentTypeString = printType(argumentType)
            buildString {
                if (variance != KmVariance.INVARIANT) {
                    append(variance.name.lowercase()).append(" ")
                }
                append(argumentTypeString)
            }
        }
    }

    val abbreviatedType = type.abbreviatedType?.let(::printType)
    val outerType = type.outerType?.let(::printType)
    val platformTypeUpperBound = type.flexibleTypeUpperBound?.let {
        @Suppress("DEPRECATION_ERROR")
        (if (it.typeFlexibilityId == JvmTypeExtensionVisitor.PLATFORM_TYPE_ID) {
            printType(it.type)
        } else null)
    }

    return buildString {
        for (annotation in type.annotations) {
            append("@").append(renderAnnotation(annotation)).append(" ")
        }
        if (type.isRaw) {
            append("/* raw */ ")
        }
        appendFlags(type.isSuspend to "suspend")
        if (outerType != null) {
            append(outerType).append(".").append(classifier.substringAfterLast('.'))
        } else {
            append(classifier)
        }
        if (arguments.isNotEmpty()) {
            arguments.joinTo(this, prefix = "<", postfix = ">")
        }
        if (type.isNullable) {
            append("?")
        }
        if (type.isDefinitelyNonNull) {
            append(" & Any")
        }
        if (abbreviatedType != null) {
            append(" /* = ").append(abbreviatedType).append(" */")
        }

        if (platformTypeUpperBound == "$this?") {
            append("!")
        } else if (platformTypeUpperBound != null) {
            append("..").append(platformTypeUpperBound)
        }
    }
}

private fun printTypeParameter(
    typeParameter: KmTypeParameter,
    settings: KotlinpSettings
): String = buildString {
    appendFlags(typeParameter.isReified to "reified")
    for (annotation in typeParameter.annotations) {
        append("@").append(renderAnnotation(annotation)).append(" ")
    }
    if (typeParameter.variance != KmVariance.INVARIANT) {
        append(typeParameter.variance.name.lowercase()).append(" ")
    }
    append("T#${typeParameter.id}")
    if (settings.isVerbose) {
        append(" /* ${typeParameter.name} */")
    }
    if (typeParameter.upperBounds.isNotEmpty()) {
        typeParameter.upperBounds.joinTo(this, separator = " & ", prefix = " : ", transform = ::printType)
    }
}

private fun printValueParameter(
    valueParameter: KmValueParameter
): String {
    val type = printType(valueParameter.type)
    val varargElementType = valueParameter.varargElementType?.let(::printType)
    return buildString {
        appendValueParameterModifiers(valueParameter)
        if (varargElementType != null) {
            append("vararg ").append(valueParameter.name).append(": ").append(varargElementType).append(" /* ").append(type).append(" */")
        } else {
            append(valueParameter.name).append(": ").append(type)
        }
        if (valueParameter.declaresDefaultValue) {
            append(" /* = ... */")
        }
    }
}

private fun renderAnnotation(annotation: KmAnnotation): String =
    annotation.className + if (annotation.arguments.isEmpty()) "" else
        annotation.arguments.entries.joinToString(prefix = "(", postfix = ")") { (name, argument) ->
            "$name = ${renderAnnotationArgument(argument)}"
        }

private fun renderAnnotationArgument(arg: KmAnnotationArgument): String =
    when (arg) {
        is KmAnnotationArgument.ByteValue -> arg.value.toString() + ".toByte()"
        is KmAnnotationArgument.CharValue -> "'${arg.value.toString().sanitize(quote = '\'')}'"
        is KmAnnotationArgument.ShortValue -> arg.value.toString() + ".toShort()"
        is KmAnnotationArgument.IntValue -> arg.value.toString()
        is KmAnnotationArgument.LongValue -> arg.value.toString() + "L"
        is KmAnnotationArgument.FloatValue -> arg.value.toString() + "f"
        is KmAnnotationArgument.DoubleValue -> arg.value.toString()
        is KmAnnotationArgument.UByteValue -> arg.value.toString() + ".toUByte()"
        is KmAnnotationArgument.UShortValue -> arg.value.toString() + ".toUShort()"
        is KmAnnotationArgument.UIntValue -> arg.value.toString() + "u"
        is KmAnnotationArgument.ULongValue -> arg.value.toString() + "uL"
        is KmAnnotationArgument.BooleanValue -> arg.value.toString()
        is KmAnnotationArgument.StringValue -> "\"${arg.value.sanitize(quote = '"')}\""
        is KmAnnotationArgument.KClassValue -> "${arg.className}::class"
        is KmAnnotationArgument.ArrayKClassValue -> buildString {
            repeat(arg.arrayDimensionCount) { append("kotlin/Array<") }
            append(arg.className).append("::class")
            repeat(arg.arrayDimensionCount) { append(">") }
        }
        is KmAnnotationArgument.EnumValue -> "${arg.enumClassName}.${arg.enumEntryName}"
        is KmAnnotationArgument.AnnotationValue -> arg.annotation.let { annotation ->
            val args = annotation.arguments.entries.joinToString { (name, argument) ->
                "$name = ${renderAnnotationArgument(argument)}"
            }
            "${annotation.className}($args)"
        }
        is KmAnnotationArgument.ArrayValue -> arg.elements.joinToString(
            prefix = "[",
            postfix = "]",
            transform = ::renderAnnotationArgument
        )
    }

private fun String.sanitize(quote: Char): String =
    buildString(length) {
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

private fun printVersionRequirement(versionRequirement: KmVersionRequirement): String {
    val version = with(versionRequirement.version) { "$major.$minor.$patch" }

    return buildString {
        append("requires ").append(
            when (versionRequirement.kind) {
                KmVersionRequirementVersionKind.LANGUAGE_VERSION -> "language version"
                KmVersionRequirementVersionKind.COMPILER_VERSION -> "compiler version"
                KmVersionRequirementVersionKind.API_VERSION -> "API version"
            }
        ).append(" ").append(version)

        listOfNotNull(
            "level=${versionRequirement.level}",
            versionRequirement.errorCode?.let { "errorCode=$it" },
            versionRequirement.message?.let { "message=\"$it\"" }
        ).joinTo(this, prefix = " (", postfix = ")")
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

private fun <T, R : Comparable<R>> Iterable<T>.sortIfNeededBy(settings: KotlinpSettings, selector: (T) -> R?): Iterable<T> {
    return if (settings.sortDeclarations) sortedBy(selector) else this
}

@ExperimentalContracts
private fun printContract(kmContract: KmContract): String = buildString {
    appendLine("contract {")
    kmContract.effects.map(::printEffect).forEach { effect ->
        appendLine("      $effect")
    }
    append("    }")
}

@ExperimentalContracts
private fun printEffect(
    kmEffect: KmEffect
): String {
    var argument: String? = null
    kmEffect.constructorArguments.forEach {
        // If there are several arguments, only the first is taken, see ContractDeserializerImpl.deserializeSimpleEffect
        if (argument == null) {
            argument = printEffectExpression(it)
        }
    }
    val conclusion: String? = kmEffect.conclusion?.let(::printEffectExpression)

    return buildString {
        when (kmEffect.type) {
            KmEffectType.RETURNS_CONSTANT -> {
                append("returns(")
                if (argument != null) {
                    append(argument)
                }
                append(")")
            }

            KmEffectType.CALLS -> {
                append("callsInPlace($argument")
                kmEffect.invocationKind?.let {
                    append(", InvocationKind.${it.name}")
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
    }
}

@ExperimentalContracts
private fun printEffectExpression(effectExpression: KmEffectExpression): String {
    val parameterIndex: Int? = effectExpression.parameterIndex
    val constantValue: List<Any?>? = effectExpression.constantValue?.let { listOf(it.value) }
    val andArguments = effectExpression.andArguments.map(::printEffectExpression)
    val orArguments = effectExpression.orArguments.map(::printEffectExpression)

    fun wrapIfNeeded(s: String): String =
        // A simple heuristic to avoid wrapping into unnecessary parentheses
        if ('&' in s || '|' in s) "($s)" else s

    return buildString {
        append(
            when {
                constantValue != null -> constantValue.single().toString()
                parameterIndex != null -> "p#$parameterIndex"
                else -> ""
            }
        )
        if (effectExpression.isInstanceType != null) {
            append(" ")
            if (effectExpression.isNegated) append("!")
            append("is ${effectExpression.isInstanceType?.let(::printType)}")
        }
        if (effectExpression.isNullCheckPredicate) {
            append(if (effectExpression.isNegated) " != " else " == ")
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
    }
}

interface AbstractPrinter<in T : KotlinClassMetadata> {
    fun print(klass: T): String
}

class ClassPrinter(private val settings: KotlinpSettings) : AbstractPrinter<KotlinClassMetadata.Class> {
    private val sb = StringBuilder()
    internal val result = StringBuilder()

    private var klass: KmClass? = null
    private var name: ClassName? = null
    private val typeParams = mutableListOf<String>()
    private val supertypes = mutableListOf<String>()
    private val contextReceiverTypes = mutableListOf<String>()
    private val versionRequirements = mutableListOf<String>()
    private var anonymousObjectOriginName: String? = null

    private fun visitEnd() {
        if (anonymousObjectOriginName != null) {
            result.appendLine("// anonymous object origin: $anonymousObjectOriginName")
        }
        for (versionRequirement in versionRequirements) {
            result.appendLine("// $versionRequirement")
        }
        if (contextReceiverTypes.isNotEmpty()) {
            result.appendLine(contextReceiverTypes.joinToString(prefix = "context(", postfix = ")"))
        }
        result.appendClassModifiers(klass!!)
        result.append(name)
        if (typeParams.isNotEmpty()) {
            typeParams.joinTo(result, prefix = "<", postfix = ">")
        }
        if (supertypes.isNotEmpty()) {
            result.append(" : ")
            supertypes.joinTo(result)
        }
        result.appendLine(" {")
        if (klass!!.hasEnumEntries) {
            sb.appendLine()
            sb.appendLine("  // has Enum.entries")
        }
        result.append(sb)
        result.appendLine("}")
    }

    private fun visitCompanionObject(name: String) {
        sb.appendLine()
        sb.appendLine("  // companion object: $name")
    }

    private fun visitNestedClass(name: String) {
        sb.appendLine()
        sb.appendLine("  // nested class: $name")
    }

    private fun visitEnumEntry(name: String) {
        sb.appendLine()
        sb.appendLine("  $name,")
    }

    private fun visitSealedSubclass(name: ClassName) {
        sb.appendLine()
        sb.appendLine("  // sealed subclass: $name")
    }

    private fun visitInlineClassUnderlyingPropertyName(name: String) {
        sb.appendLine()
        sb.appendLine("  // underlying property: $name")
    }

    private fun visitInlineClassUnderlyingType(type: String) {
        sb.appendLine()
        sb.appendLine("  // underlying type: $type")
    }

    private fun visitExtensions(kclass: KmClass) {
        val localDelegatedProperties = mutableListOf<StringBuilder>()
        val moduleName: String? = kclass.moduleName
        anonymousObjectOriginName = kclass.anonymousObjectOriginName

        kclass.localDelegatedProperties.sortIfNeededBy(settings) { it.getterSignature?.toString() ?: it.name }.forEach { p ->
            visitProperty(
                p, settings, StringBuilder().also { localDelegatedProperties.add(it) }
            )
        }

        sb.appendDeclarationContainerExtensions(settings, localDelegatedProperties, moduleName)
        if (kclass.hasMethodBodiesInInterface) {
            sb.appendLine()
            sb.appendLine("  // has method bodies in interface")
        }
        if (kclass.isCompiledInCompatibilityMode) {
            sb.appendLine()
            sb.appendLine("  // is compiled in compatibility mode")
        }
    }

    override fun print(klass: KotlinClassMetadata.Class): String = print(klass.kmClass)

    @OptIn(ExperimentalContextReceivers::class)
    fun print(kmClass: KmClass): String {
        klass = kmClass
        name = kmClass.name
        kmClass.typeParameters.forEach { typeParams.add(printTypeParameter(it, settings)) }
        supertypes.addAll(kmClass.supertypes.map { printType(it) })

        kmClass.constructors.sortIfNeededBy(settings) { it.signature.toString() }.forEach { visitConstructor(it, sb) }
        kmClass.functions.sortIfNeededBy(settings) { it.signature.toString() }.forEach { visitFunction(it, settings, sb) }
        kmClass.properties.sortIfNeededBy(settings) {
            it.getterSignature?.toString() ?: it.name
        }.forEach { visitProperty(it, settings, sb) }
        kmClass.typeAliases.sortIfNeededBy(settings) { it.name }.forEach { visitTypeAlias(it, settings, sb) }
        kmClass.companionObject?.let { visitCompanionObject(it) }
        kmClass.nestedClasses.forEach { visitNestedClass(it) }
        kmClass.enumEntries.forEach { visitEnumEntry(it) }
        kmClass.sealedSubclasses.sortIfNeededBy(settings) { it }.forEach { visitSealedSubclass(it) }
        kmClass.inlineClassUnderlyingPropertyName?.let { visitInlineClassUnderlyingPropertyName(it) }
        kmClass.inlineClassUnderlyingType?.let { visitInlineClassUnderlyingType(printType(it)) }
        kmClass.contextReceiverTypes.forEach { contextReceiverTypes.add(printType(it)) }
        kmClass.versionRequirements.forEach { versionRequirements.add(printVersionRequirement(it)) }

        visitExtensions(kmClass)
        visitEnd()
        return result.toString()
    }
}

abstract class PackagePrinter(private val settings: KotlinpSettings) {
    internal val sb = StringBuilder().apply {
        appendLine("package {")
    }

    private fun visitExtensions(kmPackage: KmPackage) {
        val localDelegatedProperties = mutableListOf<StringBuilder>()
        val moduleName: String? = kmPackage.moduleName

        kmPackage.localDelegatedProperties.sortIfNeededBy(settings) { it.getterSignature?.toString() ?: it.name }.forEach { p ->
            visitProperty(p, settings, StringBuilder().also { localDelegatedProperties.add(it) })
        }
        sb.appendDeclarationContainerExtensions(settings, localDelegatedProperties, moduleName)
    }

    fun print(kmPackage: KmPackage) {
        kmPackage.functions.sortIfNeededBy(settings) { it.signature.toString() }.forEach { visitFunction(it, settings, sb) }
        kmPackage.properties.sortIfNeededBy(settings) {
            it.getterSignature?.toString() ?: it.name
        }.forEach { visitProperty(it, settings, sb) }
        kmPackage.typeAliases.sortIfNeededBy(settings) { it.name }.forEach { visitTypeAlias(it, settings, sb) }
        visitExtensions(kmPackage)
        sb.appendLine("}")
    }
}

class FileFacadePrinter(settings: KotlinpSettings) : PackagePrinter(settings), AbstractPrinter<KotlinClassMetadata.FileFacade> {
    override fun print(klass: KotlinClassMetadata.FileFacade): String {
        print(klass.kmPackage)
        return sb.toString()
    }
}

class LambdaPrinter(private val settings: KotlinpSettings) : AbstractPrinter<KotlinClassMetadata.SyntheticClass> {
    override fun print(klass: KotlinClassMetadata.SyntheticClass): String {
        val sb = StringBuilder().apply {
            appendLine("lambda {")
        }
        val kLambda = klass.kmLambda ?: throw KotlinpException("Synthetic class $klass is not a lambda")
        visitFunction(kLambda.function, settings, sb)
        sb.appendLine("}")
        return sb.toString()
    }
}

class MultiFileClassPartPrinter(
    settings: KotlinpSettings
) : PackagePrinter(settings), AbstractPrinter<KotlinClassMetadata.MultiFileClassPart> {
    override fun print(klass: KotlinClassMetadata.MultiFileClassPart): String {
        sb.appendLine("  // facade: ${klass.facadeClassName}")
        print(klass.kmPackage)
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

class ModuleFilePrinter(private val settings: KotlinpSettings) {
    private val optionalAnnotations = mutableListOf<String>()

    private val sb = StringBuilder().apply {
        appendLine("module {")
    }

    private fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        val presentableFqName = fqName.ifEmpty { "<root>" }
        sb.appendLine("  package $presentableFqName {")
        for (fileFacade in fileFacades) {
            sb.appendLine("    $fileFacade")
        }
        for ((multiFileClassPart, facade) in multiFileClassParts) {
            sb.appendLine("    $multiFileClassPart ($facade)")
        }
        sb.appendLine("  }")
    }

    private fun visitEnd() {
        if (optionalAnnotations.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("  // Optional annotations")
            sb.appendLine()
            for (element in optionalAnnotations) {
                sb.appendLine("  " + element.replace("\n", "\n  ").trimEnd())
            }
        }
        sb.appendLine("}")
    }

    @UnstableMetadataApi
    fun print(metadata: KotlinModuleMetadata): String {
        val kmModule = metadata.kmModule
        kmModule.packageParts.forEach { (fqName, kmPackageParts) ->
            visitPackageParts(fqName, kmPackageParts.fileFacades, kmPackageParts.multiFileClassParts)
        }
//        kmModule.annotations.forEach { visitAnnotation(it) } TODO
        optionalAnnotations.addAll(kmModule.optionalAnnotationClasses.map { ClassPrinter(settings).print(it) })
        visitEnd()
        return sb.toString()
    }
}

private val VISIBILITY_MAP = mapOf(
    Visibility.INTERNAL to "internal ",
    Visibility.PRIVATE to "private ",
    Visibility.PRIVATE_TO_THIS to "private ",
    Visibility.PROTECTED to "protected ",
    Visibility.PUBLIC to "public ",
    Visibility.LOCAL to "local "
)

private val MODALITY_MAP = mapOf(
    Modality.FINAL to "final ",
    Modality.OPEN to "open ",
    Modality.ABSTRACT to "abstract ",
    Modality.SEALED to "sealed "
)

private val CLASS_KIND_MAP = mapOf(
    ClassKind.CLASS to "class ",
    ClassKind.INTERFACE to "interface ",
    ClassKind.ENUM_CLASS to "enum class ",
    ClassKind.ENUM_ENTRY to "enum entry ",
    ClassKind.ANNOTATION_CLASS to "annotation class ",
    ClassKind.OBJECT to "object ",
    ClassKind.COMPANION_OBJECT to "companion object "
)

private val MEMBER_KIND_MAP = mapOf(
    MemberKind.DECLARATION to "",
    MemberKind.FAKE_OVERRIDE to "/* fake override */ ",
    MemberKind.DELEGATION to "/* delegation */ ",
    MemberKind.SYNTHESIZED to "/* synthesized */ ",
)

private fun StringBuilder.appendFlags(vararg modifiers: Pair<Boolean, String>) = modifiers.forEach { (condition, s) ->
    if (condition) {
        append(s)
        if (s.isNotEmpty()) append(" ")
    }
}

private fun StringBuilder.appendClassModifiers(kmClass: KmClass) {
    append(VISIBILITY_MAP[kmClass.visibility])
    append(MODALITY_MAP[kmClass.modality])
    appendFlags(
        kmClass.isInner to "inner",
        kmClass.isData to "data",
        kmClass.isExternal to "external",
        kmClass.isExpect to "expect",
        kmClass.isValue to "value",
        kmClass.isFunInterface to "fun",
    )
    append(CLASS_KIND_MAP[kmClass.kind])
}

private fun StringBuilder.appendConstructorModifiers(kmConstructor: KmConstructor) {
    append(VISIBILITY_MAP[kmConstructor.visibility])
    appendFlags(
        kmConstructor.isSecondary to "/* secondary */",
        kmConstructor.hasNonStableParameterNames to "/* non-stable parameter names */"
    )
}

private fun StringBuilder.appendFunctionModifiers(kmFunction: KmFunction) {
    append(VISIBILITY_MAP[kmFunction.visibility])
    append(MODALITY_MAP[kmFunction.modality])
    append(MEMBER_KIND_MAP[kmFunction.kind])
    appendFlags(
        kmFunction.isOperator to "operator",
        kmFunction.isInfix to "infix",
        kmFunction.isInline to "inline",
        kmFunction.isTailrec to "tailrec",
        kmFunction.isExternal to "external",
        kmFunction.isSuspend to "suspend",
        kmFunction.isExpect to "expect",
        kmFunction.hasNonStableParameterNames to "/* non-stable parameter names */"
    )
}

private fun StringBuilder.appendPropertyModifiers(kmProperty: KmProperty) {
    append(VISIBILITY_MAP[kmProperty.visibility])
    append(MODALITY_MAP[kmProperty.modality])
    append(MEMBER_KIND_MAP[kmProperty.kind])
    appendFlags(
        kmProperty.isConst to "const",
        kmProperty.isLateinit to "lateinit",
        kmProperty.isExternal to "external",
        kmProperty.isDelegated to "/* delegated */",
        kmProperty.isExpect to "expect"
    )
}

private fun StringBuilder.appendPropertyAccessorModifiers(accessorAttributes: KmPropertyAccessorAttributes) {
    append(VISIBILITY_MAP[accessorAttributes.visibility])
    append(MODALITY_MAP[accessorAttributes.modality])
    appendFlags(
        accessorAttributes.isNotDefault to "/* non-default */",
        accessorAttributes.isExternal to "external",
        accessorAttributes.isInline to "inline"
    )
}

private fun StringBuilder.appendValueParameterModifiers(valueParameter: KmValueParameter) = appendFlags(
    valueParameter.isCrossinline to "crossinline",
    valueParameter.isNoinline to "noinline"
)
