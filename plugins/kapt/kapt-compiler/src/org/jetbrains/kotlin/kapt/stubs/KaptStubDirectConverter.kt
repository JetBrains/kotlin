/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import com.intellij.openapi.util.text.StringUtil
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.util.appendList
import org.jetbrains.kotlin.kapt.util.appendListIfNonEmpty
import org.jetbrains.kotlin.kapt.util.isAnnotation
import org.jetbrains.kotlin.kapt.util.isEnum
import org.jetbrains.kotlin.kapt.util.isInterface
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import javax.lang.model.element.ElementKind
import kotlin.math.sign

class KaptStubDirectConverter(
    kaptContext: KaptContextForStubGeneration,
    generateNonExistentClass: Boolean,
) : ParameterizedKaptStubConverter<String, String, String, String, String, String, String, String, String, String>(
    kaptContext,
    generateNonExistentClass
) {

    private class DirectKaptStub(
        private val packageName: String,
        private val simpleClassName: String,
        private val fileContent: String,
        kaptMetadata: ByteArray? = null,
    ) : KaptStub(kaptMetadata) {
        override fun packageName(): String = packageName

        override fun simpleClassName(): String = simpleClassName

        override fun sourceFileName(): String =
            if (packageName.isEmpty()) "$simpleClassName.java" else
                packageName.replace('.', '/') + "/$simpleClassName.java"

        override fun getText(context: Context): String = fileContent
    }

    // Whether Kapt shall generate syntactically correct Java source code, or may generate incorrect (but good for the annotation
    // processing) stubs. Currently, it is mostly a marker for the known cases of potentially incorrect syntax rather than a public flag
    private val avoidIncorrectJavaCode = false

    private fun StringBuilder.appendJavaStringLiteral(str: String) {
        append('"')
        StringUtil.escapeStringCharacters(str.length, str, "\"", this)
        append('"')
    }

    private fun StringBuilder.appendJavaCharLiteral(ch: Char) {
        append('\'')
        val str = ch.toString()
        StringUtil.escapeStringCharacters(str.length, str, "\'", this)
        append('\'')
    }

    private fun StringBuilder.appendTypeArguments(typeArgs: List<String>) =
        appendListIfNonEmpty(typeArgs, "<", ">")

    private fun StringBuilder.appendModifiers(flags: Long, kind: ElementKind) {
        fun appendModifierIfPresent(flag: Long, modifier: String) {
            if (flags and flag != 0L) {
                append(modifier).append(" ")
            }
        }

        fun appendModifierIfPresent(flag: Int, modifier: String) = appendModifierIfPresent(flag.toLong(), modifier)

        appendModifierIfPresent(Opcodes.ACC_PUBLIC, "public")
        appendModifierIfPresent(Opcodes.ACC_PROTECTED, "protected")
        appendModifierIfPresent(Opcodes.ACC_PRIVATE, "private")
        appendModifierIfPresent(Opcodes.ACC_STATIC, "static")
        appendModifierIfPresent(Opcodes.ACC_ABSTRACT, "abstract")
        appendModifierIfPresent(Opcodes.ACC_FINAL, "final")
        appendModifierIfPresent(Opcodes.ACC_NATIVE, "native")
        appendModifierIfPresent(Opcodes.ACC_SYNCHRONIZED, "synchronized")
        if (kind == ElementKind.FIELD) {
            appendModifierIfPresent(Opcodes.ACC_TRANSIENT, "transient")
        } // varargs for methods have the same code, but do not contribute to modifiers
        appendModifierIfPresent(Opcodes.ACC_VOLATILE, "volatile")
        appendModifierIfPresent(Opcodes.ACC_STRICT, "strictfp")
        appendModifierIfPresent(Flags.DEFAULT, "default")
    }

    private fun StringBuilder.appendKDocCommentIfNecessary(node: Any) {
        val origin = kaptContext.origins[node] ?: return
        val psiElement = origin.element as? KtDeclaration ?: return
        val docComment = psiElement.docComment ?: return

        if (origin.declaration is IrConstructor && psiElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return
        }

        appendKDocComment(extractComment(docComment))
    }

    private fun StringBuilder.appendKDocComment(text: String) {
        append("/**\n")
        for (line in text.lines()) {
            append(" *")
            if (line.firstOrNull()?.let { it > ' ' } == true) {
                append(' ')
            }
            append(line).append('\n')
        }
        append(" */\n")
    }

    override fun makeNonExistentClassStub(): KaptStub = DirectKaptStub(
        NON_EXISTENT_CLASS_NAME.parent().asString(),
        NON_EXISTENT_CLASS_NAME.shortName().asString(),
        "package ${NON_EXISTENT_CLASS_NAME.parent().asString()};\n\n" +
                "public final class ${NON_EXISTENT_CLASS_NAME.shortName().asString()} {\n}\n"
    )

    override fun makeStubForTopLevelClass(
        declaration: IrDeclaration,
        lineMappings: KaptLineMappingCollector,
        packageName: String,
        clazz: ClassNode,
    ): KaptStub? {
        val [simpleName, classText] = convertClass(clazz, lineMappings, packageName) ?: return null

        val firFile = findFirFile(declaration)
        val imports: List<String> = convertImports(firFile, simpleName)

        val text = buildString {
            if (packageName.isNotEmpty()) {
                append("package ").append(packageName).append(";\n\n")
            }
            for (import in imports) {
                append(import)
            }
            if (imports.isNotEmpty()) {
                append("\n")
            }
            append(classText)
        }

        return DirectKaptStub(packageName, simpleName, text, lineMappings.serialize())
    }

    override fun makeTypeParameter(name: String, allBounds: List<String>): String = buildString {
        append(name)
        if (allBounds.isNotEmpty()) {
            allBounds.joinTo(this, prefix = " extends ", separator = " & ")
        }
    }

    override fun makeWildcard(asmWilcard: Char, bound: String): String = when (asmWilcard) {
        '=' -> bound
        '+' -> "? extends $bound"
        '-' -> "? super $bound"
        else -> error("Unknown variance, '=', '+' or '-' expected")
    }

    override fun makeUnboundWildcard(): String = "?"

    override fun makeField(
        field: FieldNode,
        modifiers: String,
        initializer: String?,
        convertedType: String,
    ): String = buildString {
        val isEnumField = isEnum(field.access)
        if (!isEnumField) {
            appendKDocCommentIfNecessary(field)
        }
        append(modifiers)
        if (isEnumField) {
            append(initializer ?: field.name)
            append(",\n")
        } else {
            append(convertedType).append(" ")
            append(field.name)
            initializer?.let { append(" = ").append(it) }
            append(";\n")
        }
    }

    override fun makeEnumValueInitializer(
        name: String,
        args: List<String>,
        def: String?,
    ): String = buildString {
        append(name)
        appendListIfNonEmpty(args, "(", ")")
    }

    override fun makeClassDecl(
        clazz: ClassNode,
        simpleName: String,
        modifiers: String,
        genericType: SignatureParser.ClassGenericSignature<String, String>,
        superTypes: ClassSupertypes<String>,
        enumValues: List<String>,
        sortedConvertedFields: List<String>,
        sortedConvertedMethods: List<String>,
        nestedClasses: List<String>,
    ): String {
        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val text = buildString {
            appendKDocCommentIfNecessary(clazz)
            append(modifiers)
            val classKindText = when {
                isEnum -> "enum"
                isAnnotation -> "@interface"
                clazz.isInterface() -> "interface"
                else -> "class"
            }
            append(classKindText).append(" ").append(simpleName)
            if (!isAnnotation || !avoidIncorrectJavaCode) {
                // interface cannot have type parameters or extends clause, but they are allowed (not reported)
                // during the annotations processing
                appendTypeArguments(genericType.typeParameters)
                if (clazz.isInterface()) {
                    require(superTypes.superClass == null) {
                        "Interface ${clazz.name} has an unexpected superclass in Java text generation"
                    }
                    appendListIfNonEmpty(superTypes.interfaces, " extends ", "") { it }
                } else {
                    superTypes.superClass?.let {
                        append(" extends ").append(it)
                    }
                    appendListIfNonEmpty(superTypes.interfaces, " implements ", "") { it }
                }
            }
            append(" {\n")
            if (isEnum) {
                for (enumValue in enumValues)
                    append(enumValue)
                append(";\n")
            }
            for (field in sortedConvertedFields)
                append(field).append("\n")
            for (method in sortedConvertedMethods)
                append(method).append("\n")
            for (nestedClass in nestedClasses)
                append(nestedClass).append("\n")
            append("}\n")
        }

        return text
    }

    override fun makeSingleImport(fqName: FqName): String = "import ${fqName.asString()};\n"

    override fun makeStarImport(fqName: FqName): String = "import ${fqName.asString()}.*;\n"

    override fun makeParameter(
        name: String,
        type: String,
        modifiers: String,
        isVararg: Boolean,
    ): String = buildString {
        append(modifiers)
        append(if (isVararg) type.removeSuffix("[]") + "..." else type)
        append(' ')
        append(name)
    }

    override fun makeBlock(statements: List<String>): String =
        statements.joinToString(separator = "\n") { it }

    override fun makeSimpleCallStatement(name: String, args: List<String>): String = buildString {
        append(name)
        args.joinTo(this, separator = ", ", prefix = "(", postfix = ");")
    }

    override fun makeReturn(arg: String): String = "return $arg;"
    override fun makeTypeApply(type: String, typeArguments: List<String>): String = buildString {
        append(type)
        typeArguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
    }

    override fun makePrimitiveType(primitiveTypeSig: Char): String = when (primitiveTypeSig) {
        'V' -> "void"
        'Z' -> "boolean"
        'C' -> "char"
        'B' -> "byte"
        'S' -> "short"
        'I' -> "int"
        'F' -> "float"
        'J' -> "long"
        'D' -> "double"
        else -> error("Illegal primitive type $primitiveTypeSig")
    }

    override fun makeMethod(
        method: MethodNode,
        irClass: IrClass,
        modifiers: String,
        genericSignature: SignatureParser.MethodGenericSignature<String, String>,
        parameters: List<String>,
        exceptionTypes: List<String>,
        body: String?,
        defaultValue: String?,
    ): String {
        val isConstructor = method.name == "<init>"

        return buildString {
            appendKDocCommentIfNecessary(method)
            append(modifiers)

            if (isConstructor) {
                append(irClass.name)
            } else {
                appendListIfNonEmpty(genericSignature.typeParameters, "<", ">")
                append(genericSignature.returnType!!)
                append(" ")
                append(method.name)
            }
            appendList(parameters, "(", ")")
            appendListIfNonEmpty(exceptionTypes, " throws ", "")
            if (body != null) {
                append(" {\n").append(body).append("\n}")
            } else if (defaultValue != null) {
                append(" default ").append(defaultValue).append(";")
            } else {
                append(";")
            }
        }
    }

    override fun makeAssignExpression(name: String, expression: String) = "$name = $expression"

    override fun makeAnnotation(type: String, arguments: List<String>): String = buildString {
        append('@')
        append(type)
        appendListIfNonEmpty(arguments, "(", ")")
        append("\n")
    }

    override fun makeArrayType(baseType: String, arrayDimensions: Int) = baseType + "[]".repeat(arrayDimensions)

    override fun makeSimpleName(simpleName: String) = simpleName

    override fun makeSelect(left: String, simpleName: String) = "$left.$simpleName"

    override fun makeType(asmType: Type): String = treeMaker.convertAsmTypeToJavaText(asmType)

    override fun makeQualifiedName(javaName: String): String = treeMaker.getQualifiedName(javaName)

    override fun makeQualifiedName(fqName: FqName): String = fqName.asString()

    override fun isNonExistentClass(type: String): Boolean = type == NON_EXISTENT_CLASS_NAME.asString()

    override fun makeArray(expressions: List<String>): String = expressions.joinToString(prefix = "{", postfix = "}")

    override fun makeValueOfPrimitiveTypeOrString(value: Any?): String? {
        fun specialFpValueNumerator(value: Double): Double = if (value.isNaN()) 0.0 else 1.0 * value.sign

        val sb = StringBuilder()
        when (value) {
            is Char ->
                sb.appendJavaCharLiteral(value)
            is Byte ->
                sb.append("(byte)").append(value.toInt())
            is Short ->
                sb.append("(short)").append(value.toInt())
            is Boolean, is Int ->
                sb.append(value)
            is Long ->
                sb.append(value).append("L")
            is String ->
                sb.appendJavaStringLiteral(value)
            is Float if value.isFinite() ->
                sb.append(value.toString()).append("F")
            is Float ->
                sb.append(specialFpValueNumerator(value.toDouble())).append("F / 0.0F")
            is Double if value.isFinite() ->
                sb.append(value.toString())
            is Double ->
                sb.append(specialFpValueNumerator(value)).append(" / 0.0")
            is UByte ->
                sb.append("(byte)").append(value.toInt())
            is UShort ->
                sb.append("(short)").append(value.toInt())
            is UInt ->
                sb.append(value.toInt())
            is ULong ->
                sb.append(value.toLong()).append("L")
            else ->
                return null
        }

        return sb.toString()
    }

    override fun makeNullLiteral(): String = "null"

    override fun makeModifiers(kind: ElementKind, isEnumField: Boolean, flags: Long, annotations: List<String>): String = buildString {
        for (annotation in annotations)
            append(annotation)
        if (!isEnumField)
            appendModifiers(flags, kind)
    }
}
