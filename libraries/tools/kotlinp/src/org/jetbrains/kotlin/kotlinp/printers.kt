/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
        var jvmDesc: JvmMemberSignature? = null
        var lambdaClassOriginName: String? = null

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

        override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
            if (type != JvmFunctionExtensionVisitor.TYPE) return null
            return object : JvmFunctionExtensionVisitor() {
                override fun visit(desc: JvmMethodSignature?) {
                    jvmDesc = desc
                }

                override fun visitLambdaClassOriginName(internalName: String) {
                    lambdaClassOriginName = internalName
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            if (lambdaClassOriginName != null) {
                sb.appendln("  // lambda class origin: $lambdaClassOriginName")
            }
            for (versionRequirement in versionRequirements) {
                sb.appendln("  // $versionRequirement")
            }
            if (jvmDesc != null) {
                sb.appendln("  // signature: $jvmDesc")
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
            sb.appendln()
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
        var jvmFieldDesc: JvmMemberSignature? = null
        var jvmGetterDesc: JvmMemberSignature? = null
        var jvmSetterDesc: JvmMemberSignature? = null
        var jvmSyntheticMethodForAnnotationsDesc: JvmMemberSignature? = null

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
                override fun visit(fieldDesc: JvmFieldSignature?, getterDesc: JvmMethodSignature?, setterDesc: JvmMethodSignature?) {
                    jvmFieldDesc = fieldDesc
                    jvmGetterDesc = getterDesc
                    jvmSetterDesc = setterDesc
                }

                override fun visitSyntheticMethodForAnnotations(desc: JvmMethodSignature?) {
                    jvmSyntheticMethodForAnnotationsDesc = desc
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            for (versionRequirement in versionRequirements) {
                sb.appendln("  // $versionRequirement")
            }
            if (jvmFieldDesc != null) {
                sb.appendln("  // field: $jvmFieldDesc")
            }
            if (jvmGetterDesc != null) {
                sb.appendln("  // getter: $jvmGetterDesc")
            }
            if (jvmSetterDesc != null) {
                sb.appendln("  // setter: $jvmSetterDesc")
            }
            if (jvmSyntheticMethodForAnnotationsDesc != null) {
                sb.appendln("  // synthetic method for annotations: $jvmSyntheticMethodForAnnotationsDesc")
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
            sb.appendln()
            if (Flag.Property.HAS_GETTER(flags)) {
                sb.append("    ")
                sb.appendFlags(getterFlags, PROPERTY_ACCESSOR_FLAGS_MAP)
                sb.appendln("get")
            }
            if (Flag.Property.HAS_SETTER(flags)) {
                sb.append("    ")
                sb.appendFlags(setterFlags, PROPERTY_ACCESSOR_FLAGS_MAP)
                sb.append("set")
                if (setterParameter != null) {
                    sb.append("(").append(setterParameter).append(")")
                }
                sb.appendln()
            }
        }
    }

private fun visitConstructor(sb: StringBuilder, flags: Flags): KmConstructorVisitor =
    object : KmConstructorVisitor() {
        val params = mutableListOf<String>()
        val versionRequirements = mutableListOf<String>()
        var jvmDesc: JvmMemberSignature? = null

        override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { params.add(it) }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirements.add(it) }

        override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
            if (type != JvmConstructorExtensionVisitor.TYPE) return null
            return object : JvmConstructorExtensionVisitor() {
                override fun visit(desc: JvmMethodSignature?) {
                    jvmDesc = desc
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            for (versionRequirement in versionRequirements) {
                sb.appendln("  // $versionRequirement")
            }
            if (jvmDesc != null) {
                sb.appendln("  // signature: $jvmDesc")
            }
            sb.append("  ")
            sb.appendFlags(flags, CONSTRUCTOR_FLAGS_MAP)
            sb.append("constructor(")
            params.joinTo(sb)
            sb.appendln(")")
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
            sb.appendln()
            for (versionRequirement in versionRequirements) {
                sb.appendln("  // $versionRequirement")
            }
            for (annotation in annotations) {
                sb.append("  ").append("@").append(renderAnnotation(annotation)).appendln()
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
            sb.appendln()
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

private fun renderAnnotationArgument(arg: KmAnnotationArgument<*>): String =
    when (arg) {
        is KmAnnotationArgument.ByteValue -> arg.value.toString() + ".toByte()"
        is KmAnnotationArgument.CharValue -> "'${arg.value.toString().sanitize(quote = '\'')}'"
        is KmAnnotationArgument.ShortValue -> arg.value.toString() + ".toShort()"
        is KmAnnotationArgument.IntValue -> arg.value.toString()
        is KmAnnotationArgument.LongValue -> arg.value.toString() + "L"
        is KmAnnotationArgument.FloatValue -> arg.value.toString() + "f"
        is KmAnnotationArgument.DoubleValue -> arg.value.toString()
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

private fun StringBuilder.appendLocalDelegatedProperties(localDelegatedProperties: List<StringBuilder>) {
    for ((i, sb) in localDelegatedProperties.withIndex()) {
        appendln()
        appendln("  // local delegated property #$i")
        for (line in sb.lineSequence()) {
            if (line.isBlank()) continue
            // Comment all uncommented lines to not make it look like these properties are declared here
            appendln(
                if (line.startsWith("  ") && !line.startsWith("  //")) line.replaceFirst("  ", "  // ")
                else line
            )
        }
    }
}

interface AbstractPrinter<in T : KotlinClassMetadata> {
    fun print(klass: T): String
}

class ClassPrinter(private val settings: KotlinpSettings) : KmClassVisitor(), AbstractPrinter<KotlinClassMetadata.Class> {
    private val sb = StringBuilder()
    private val result = StringBuilder()

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
            result.appendln("// anonymous object origin: $anonymousObjectOriginName")
        }
        for (versionRequirement in versionRequirements) {
            result.appendln("// $versionRequirement")
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
        result.appendln(" {")
        result.append(sb)
        result.appendln("}")
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
        sb.appendln()
        sb.appendln("  // companion object: $name")
    }

    override fun visitNestedClass(name: String) {
        sb.appendln()
        sb.appendln("  // nested class: $name")
    }

    override fun visitEnumEntry(name: String) {
        sb.appendln()
        sb.appendln("  $name,")
    }

    override fun visitSealedSubclass(name: ClassName) {
        sb.appendln()
        sb.appendln("  // sealed subclass: $name")
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        printVersionRequirement { versionRequirements.add(it) }

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? {
        if (type != JvmClassExtensionVisitor.TYPE) return null
        return object : JvmClassExtensionVisitor() {
            private val localDelegatedProperties = mutableListOf<StringBuilder>()

            override fun visitAnonymousObjectOriginName(internalName: String) {
                anonymousObjectOriginName = internalName
            }

            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = visitProperty(
                settings, StringBuilder().also { localDelegatedProperties.add(it) }, flags, name, getterFlags, setterFlags
            )

            override fun visitEnd() {
                sb.appendLocalDelegatedProperties(localDelegatedProperties)
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
        appendln("package {")
    }

    override fun visitEnd() {
        sb.appendln("}")
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

            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = visitProperty(
                settings, StringBuilder().also { localDelegatedProperties.add(it) }, flags, name, getterFlags, setterFlags
            )

            override fun visitEnd() {
                sb.appendLocalDelegatedProperties(localDelegatedProperties)
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
        appendln("lambda {")
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        visitFunction(settings, sb, flags, name)

    override fun visitEnd() {
        sb.appendln("}")
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
        sb.appendln("  // facade: ${klass.facadeClassName}")
        klass.accept(this)
        return sb.toString()
    }
}

class MultiFileClassFacadePrinter : AbstractPrinter<KotlinClassMetadata.MultiFileClassFacade> {
    override fun print(klass: KotlinClassMetadata.MultiFileClassFacade): String =
        buildString {
            appendln("multi-file class {")
            for (part in klass.partClassNames) {
                appendln("  // $part")
            }
            appendln("}")
        }
}

class ModuleFilePrinter : KmModuleVisitor() {
    private val sb = StringBuilder().apply {
        appendln("module {")
    }

    override fun visitPackageParts(fqName: String, fileFacades: List<String>, multiFileClassParts: Map<String, String>) {
        val presentableFqName = if (fqName.isEmpty()) "<root>" else fqName
        sb.appendln("  package $presentableFqName {")
        for (fileFacade in fileFacades) {
            sb.appendln("    $fileFacade")
        }
        for ((multiFileClassPart, facade) in multiFileClassParts) {
            sb.appendln("    $multiFileClassPart ($facade)")
        }
        sb.appendln("  }")
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        // TODO
    }

    override fun visitEnd() {
        sb.appendln("}")
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

    Flag.Class.IS_CLASS to "class",
    Flag.Class.IS_INTERFACE to "interface",
    Flag.Class.IS_ENUM_CLASS to "enum class",
    Flag.Class.IS_ENUM_ENTRY to "enum entry",
    Flag.Class.IS_ANNOTATION_CLASS to "annotation class",
    Flag.Class.IS_OBJECT to "object",
    Flag.Class.IS_COMPANION_OBJECT to "companion object"
)

private val CONSTRUCTOR_FLAGS_MAP = VISIBILITY_FLAGS_MAP + mapOf(
    Flag.Constructor.IS_PRIMARY to "/* primary */"
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
    Flag.Function.IS_EXPECT to "expect"
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
