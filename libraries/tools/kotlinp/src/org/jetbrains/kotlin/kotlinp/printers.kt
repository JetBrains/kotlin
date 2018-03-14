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

private fun visitFunction(sb: StringBuilder, flags: Int, name: String): KmFunctionVisitor =
    object : KmFunctionVisitor() {
        val typeParams = mutableListOf<String>()
        val params = mutableListOf<String>()
        var receiverParameterType: String? = null
        var returnType: String? = null
        var versionRequirement: String? = null
        var jvmDesc: String? = null

        override fun visitReceiverParameterType(flags: Int): KmTypeVisitor? =
            printType(flags) { receiverParameterType = it }

        override fun visitTypeParameter(
            flags: Int, name: String, id: Int, variance: KmVariance
        ): KmTypeParameterVisitor? =
            printTypeParameter(flags, name, id, variance) { typeParams.add(it) }

        override fun visitValueParameter(flags: Int, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { params.add(it) }

        override fun visitReturnType(flags: Int): KmTypeVisitor? =
            printType(flags) { returnType = it }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirement = it }

        override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
            if (type != JvmFunctionExtensionVisitor.TYPE) return null
            return object : JvmFunctionExtensionVisitor() {
                override fun visit(desc: String?) {
                    jvmDesc = desc
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            if (versionRequirement != null) {
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

private fun visitProperty(sb: StringBuilder, flags: Int, name: String, getterFlags: Int, setterFlags: Int): KmPropertyVisitor =
    object : KmPropertyVisitor() {
        val typeParams = mutableListOf<String>()
        var receiverParameterType: String? = null
        var returnType: String? = null
        var setterParameter: String? = null
        var versionRequirement: String? = null
        var jvmFieldName: String? = null
        var jvmFieldTypeDesc: String? = null
        var jvmGetterDesc: String? = null
        var jvmSetterDesc: String? = null
        var jvmSyntheticMethodForAnnotationsDesc: String? = null

        override fun visitReceiverParameterType(flags: Int): KmTypeVisitor? =
            printType(flags) { receiverParameterType = it }

        override fun visitTypeParameter(flags: Int, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
            printTypeParameter(flags, name, id, variance) { typeParams.add(it) }

        override fun visitSetterParameter(flags: Int, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { setterParameter = it }

        override fun visitReturnType(flags: Int): KmTypeVisitor? =
            printType(flags) { returnType = it }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirement = it }

        override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
            if (type != JvmPropertyExtensionVisitor.TYPE) return null
            return object : JvmPropertyExtensionVisitor() {
                override fun visit(fieldName: String?, fieldTypeDesc: String?, getterDesc: String?, setterDesc: String?) {
                    jvmFieldName = fieldName
                    jvmFieldTypeDesc = fieldTypeDesc
                    jvmGetterDesc = getterDesc
                    jvmSetterDesc = setterDesc
                }

                override fun visitSyntheticMethodForAnnotations(desc: String?) {
                    jvmSyntheticMethodForAnnotationsDesc = desc
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            if (versionRequirement != null) {
                sb.appendln("  // $versionRequirement")
            }
            if (jvmFieldName != null || jvmFieldTypeDesc != null) {
                sb.append("  // field: ${jvmFieldName ?: "<null>"}")
                if (jvmFieldTypeDesc != null) {
                    sb.append(":$jvmFieldTypeDesc")
                }
                sb.appendln()
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
            sb.append(if (Flags.Property.IS_VAR(flags)) "var " else "val ")
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
            if (Flags.Property.HAS_CONSTANT(flags)) {
                sb.append(" /* = ... */")
            }
            sb.appendln()
            if (Flags.Property.HAS_GETTER(flags)) {
                sb.append("    ")
                sb.appendFlags(getterFlags, PROPERTY_ACCESSOR_FLAGS_MAP)
                sb.appendln("get")
            }
            if (Flags.Property.HAS_SETTER(flags)) {
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

private fun visitConstructor(sb: StringBuilder, flags: Int): KmConstructorVisitor =
    object : KmConstructorVisitor() {
        val params = mutableListOf<String>()
        var versionRequirement: String? = null
        var jvmDesc: String? = null

        override fun visitValueParameter(flags: Int, name: String): KmValueParameterVisitor? =
            printValueParameter(flags, name) { params.add(it) }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirement = it }

        override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
            if (type != JvmConstructorExtensionVisitor.TYPE) return null
            return object : JvmConstructorExtensionVisitor() {
                override fun visit(desc: String?) {
                    jvmDesc = desc
                }
            }
        }

        override fun visitEnd() {
            sb.appendln()
            if (versionRequirement != null) {
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

private fun visitTypeAlias(sb: StringBuilder, flags: Int, name: String): KmTypeAliasVisitor =
    object : KmTypeAliasVisitor() {
        val annotations = mutableListOf<KmAnnotation>()
        val typeParams = mutableListOf<String>()
        var underlyingType: String? = null
        var expandedType: String? = null
        var versionRequirement: String? = null

        override fun visitTypeParameter(flags: Int, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
            printTypeParameter(flags, name, id, variance) { typeParams.add(it) }

        override fun visitUnderlyingType(flags: Int): KmTypeVisitor? =
            printType(flags) { underlyingType = it }

        override fun visitExpandedType(flags: Int): KmTypeVisitor? =
            printType(flags) { expandedType = it }

        override fun visitAnnotation(annotation: KmAnnotation) {
            annotations += annotation
        }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            printVersionRequirement { versionRequirement = it }

        override fun visitEnd() {
            sb.appendln()
            if (versionRequirement != null) {
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

private fun printType(flags: Int, output: (String) -> Unit): KmTypeVisitor =
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

        override fun visitAbbreviatedType(flags: Int): KmTypeVisitor? =
            printType(flags) { abbreviatedType = it }

        override fun visitArgument(flags: Int, variance: KmVariance): KmTypeVisitor? =
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

        override fun visitOuterType(flags: Int): KmTypeVisitor? =
            printType(flags) { outerType = it }

        override fun visitFlexibleTypeUpperBound(flags: Int, typeFlexibilityId: String?): KmTypeVisitor? =
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
                if (Flags.Type.IS_NULLABLE(flags)) {
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

private fun printTypeParameter(flags: Int, name: String, id: Int, variance: KmVariance, output: (String) -> Unit): KmTypeParameterVisitor =
    object : KmTypeParameterVisitor() {
        val bounds = mutableListOf<String>()
        val jvmAnnotations = mutableListOf<KmAnnotation>()

        override fun visitUpperBound(flags: Int): KmTypeVisitor? =
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
                append("T#$id /* $name */")
                if (bounds.isNotEmpty()) {
                    bounds.joinTo(this, separator = " & ", prefix = " : ")
                }
            })
        }
    }

private fun printValueParameter(flags: Int, name: String, output: (String) -> Unit): KmValueParameterVisitor =
    object : KmValueParameterVisitor() {
        var varargElementType: String? = null
        var type: String? = null

        override fun visitType(flags: Int): KmTypeVisitor? =
            printType(flags) { type = it }

        override fun visitVarargElementType(flags: Int): KmTypeVisitor? =
            printType(flags) { varargElementType = it }

        override fun visitEnd() {
            output(buildString {
                appendFlags(flags, VALUE_PARAMETER_FLAGS_MAP)
                if (varargElementType != null) {
                    append("vararg ").append(name).append(": ").append(varargElementType).append(" /* ").append(type).append(" */")
                } else {
                    append(name).append(": ").append(type)
                }
                if (Flags.ValueParameter.DECLARES_DEFAULT_VALUE(flags)) {
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

private fun StringBuilder.appendFlags(flags: Int, map: Map<MetadataFlag, String>) {
    for ((modifier, string) in map) {
        if (modifier(flags)) {
            append(string)
            if (string.isNotEmpty()) append(" ")
        }
    }
}

interface AbstractPrinter<in T : KotlinClassMetadata> {
    fun print(klass: T): String
}

class ClassPrinter : KmClassVisitor(), AbstractPrinter<KotlinClassMetadata.Class> {
    private val sb = StringBuilder()
    private val result = StringBuilder()

    private var flags: Int? = null
    private var name: ClassName? = null
    private val typeParams = mutableListOf<String>()
    private val supertypes = mutableListOf<String>()
    private var versionRequirement: String? = null

    override fun visit(flags: Int, name: ClassName) {
        this.flags = flags
        this.name = name
    }

    override fun visitEnd() {
        if (versionRequirement != null) {
            result.appendln("  // $versionRequirement")
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

    override fun visitTypeParameter(flags: Int, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        printTypeParameter(flags, name, id, variance) { typeParams.add(it) }

    override fun visitSupertype(flags: Int): KmTypeVisitor? =
        printType(flags) { supertypes.add(it) }

    override fun visitConstructor(flags: Int): KmConstructorVisitor? =
        visitConstructor(sb, flags)

    override fun visitFunction(flags: Int, name: String): KmFunctionVisitor? =
        visitFunction(sb, flags, name)

    override fun visitProperty(flags: Int, name: String, getterFlags: Int, setterFlags: Int): KmPropertyVisitor? =
        visitProperty(sb, flags, name, getterFlags, setterFlags)

    override fun visitTypeAlias(flags: Int, name: String): KmTypeAliasVisitor? =
        visitTypeAlias(sb, flags, name)

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
        printVersionRequirement { versionRequirement = it }

    override fun print(klass: KotlinClassMetadata.Class): String {
        klass.accept(this)
        return result.toString()
    }
}

abstract class PackagePrinter : KmPackageVisitor() {
    internal val sb = StringBuilder().apply {
        appendln("package {")
    }

    override fun visitEnd() {
        sb.appendln("}")
    }

    override fun visitFunction(flags: Int, name: String): KmFunctionVisitor? =
        visitFunction(sb, flags, name)

    override fun visitProperty(flags: Int, name: String, getterFlags: Int, setterFlags: Int): KmPropertyVisitor? =
        visitProperty(sb, flags, name, getterFlags, setterFlags)

    override fun visitTypeAlias(flags: Int, name: String): KmTypeAliasVisitor? =
        visitTypeAlias(sb, flags, name)
}

class FileFacadePrinter : PackagePrinter(), AbstractPrinter<KotlinClassMetadata.FileFacade> {
    override fun print(klass: KotlinClassMetadata.FileFacade): String {
        klass.accept(this)
        return sb.toString()
    }
}

class LambdaPrinter : KmLambdaVisitor(), AbstractPrinter<KotlinClassMetadata.SyntheticClass> {
    private val sb = StringBuilder().apply {
        appendln("lambda {")
    }

    override fun visitFunction(flags: Int, name: String): KmFunctionVisitor? =
        visitFunction(sb, flags, name)

    override fun visitEnd() {
        sb.appendln("}")
    }

    override fun print(klass: KotlinClassMetadata.SyntheticClass): String {
        klass.accept(this)
        return sb.toString()
    }
}

class MultiFileClassPartPrinter : PackagePrinter(), AbstractPrinter<KotlinClassMetadata.MultiFileClassPart> {
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
    Flags.IS_INTERNAL to "internal",
    Flags.IS_PRIVATE to "private",
    Flags.IS_PRIVATE_TO_THIS to "private",
    Flags.IS_PROTECTED to "protected",
    Flags.IS_PUBLIC to "public",
    Flags.IS_LOCAL to "local"
)

private val COMMON_FLAGS_MAP = VISIBILITY_FLAGS_MAP + mapOf(
    Flags.IS_FINAL to "final",
    Flags.IS_OPEN to "open",
    Flags.IS_ABSTRACT to "abstract",
    Flags.IS_SEALED to "sealed"
)

private val CLASS_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flags.Class.IS_INNER to "inner",
    Flags.Class.IS_DATA to "data",
    Flags.Class.IS_EXTERNAL to "external",
    Flags.Class.IS_EXPECT to "expect",
    Flags.Class.IS_INLINE to "inline",

    Flags.Class.IS_CLASS to "class",
    Flags.Class.IS_INTERFACE to "interface",
    Flags.Class.IS_ENUM_CLASS to "enum class",
    Flags.Class.IS_ENUM_ENTRY to "enum entry",
    Flags.Class.IS_ANNOTATION_CLASS to "annotation class",
    Flags.Class.IS_OBJECT to "object",
    Flags.Class.IS_COMPANION_OBJECT to "companion object"
)

private val CONSTRUCTOR_FLAGS_MAP = VISIBILITY_FLAGS_MAP + mapOf(
    Flags.Constructor.IS_PRIMARY to "/* primary */"
)

private val FUNCTION_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flags.Function.IS_DECLARATION to "",
    Flags.Function.IS_FAKE_OVERRIDE to "/* fake override */",
    Flags.Function.IS_DELEGATION to "/* delegation */",
    Flags.Function.IS_SYNTHESIZED to "/* synthesized */",

    Flags.Function.IS_OPERATOR to "operator",
    Flags.Function.IS_INFIX to "infix",
    Flags.Function.IS_INLINE to "inline",
    Flags.Function.IS_TAILREC to "tailrec",
    Flags.Function.IS_EXTERNAL to "external",
    Flags.Function.IS_SUSPEND to "suspend",
    Flags.Function.IS_EXPECT to "expect"
)

private val PROPERTY_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flags.Property.IS_DECLARATION to "",
    Flags.Property.IS_FAKE_OVERRIDE to "/* fake override */",
    Flags.Property.IS_DELEGATION to "/* delegation */",
    Flags.Property.IS_SYNTHESIZED to "/* synthesized */",

    Flags.Property.IS_CONST to "const",
    Flags.Property.IS_LATEINIT to "lateinit",
    Flags.Property.IS_EXTERNAL to "external",
    Flags.Property.IS_DELEGATED to "/* delegated */",
    Flags.Property.IS_EXPECT to "expect"
)

private val PROPERTY_ACCESSOR_FLAGS_MAP = COMMON_FLAGS_MAP + mapOf(
    Flags.PropertyAccessor.IS_NOT_DEFAULT to "/* non-default */",
    Flags.PropertyAccessor.IS_EXTERNAL to "external",
    Flags.PropertyAccessor.IS_INLINE to "inline"
)

private val VALUE_PARAMETER_FLAGS_MAP = mapOf(
    Flags.ValueParameter.IS_CROSSINLINE to "crossinline",
    Flags.ValueParameter.IS_NOINLINE to "noinline"
)

private val TYPE_PARAMETER_FLAGS_MAP = mapOf(
    Flags.TypeParameter.IS_REIFIED to "reified"
)

private val TYPE_FLAGS_MAP = mapOf(
    Flags.Type.IS_SUSPEND to "suspend"
)
