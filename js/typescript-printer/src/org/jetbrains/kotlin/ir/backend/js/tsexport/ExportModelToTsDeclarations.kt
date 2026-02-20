/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf


private const val NonNullable = "NonNullable"
private const val declare = "declare "
private const val default = "default "
private const val export = "export "

private const val getInstance = "getInstance"

private const val Metadata = $$"$metadata$"
private const val MetadataType = "type"
private const val MetadataConstructor = "constructor"
private const val Nullable = "Nullable"
private const val ObjectInheritanceIntrinsic = "KtSingleton"

@JvmInline
public value class TypeScriptFragment(public val raw: String)

public fun List<ExportedDeclaration>.toTypeScriptFragment(moduleKind: ModuleKind): TypeScriptFragment {
    return ExportModelToTsDeclarations(moduleKind).generateTypeScriptFragment(this)
}

public fun List<TypeScriptFragment>.joinTypeScriptFragments(): TypeScriptFragment {
    return TypeScriptFragment(joinToString("\n") { it.raw })
}

public fun List<TypeScriptFragment>.toTypeScript(name: String, moduleKind: ModuleKind): String {
    return ExportModelToTsDeclarations(moduleKind).generateTypeScript(name, this)
}

// TODO: Support module kinds other than plain
public class ExportModelToTsDeclarations(private val moduleKind: ModuleKind) {
    private val isEsModules = moduleKind == ModuleKind.ES
    private val intrinsicsPrefix = if (moduleKind == ModuleKind.PLAIN) "" else declare
    private val indent: String = if (moduleKind == ModuleKind.PLAIN) "    " else ""
    private val defaultExportPrefix = if (moduleKind == ModuleKind.PLAIN) "" else "$export$default"

    private val ExportedDeclaration.topLevelPrefix: String
        get() = when {
            moduleKind == ModuleKind.PLAIN -> ""
            isDefaultExport -> declare
            else -> "$export$declare"
        }

    public fun generateTypeScript(name: String, declarations: List<TypeScriptFragment>): String {
        val internalNamespace = """
            type $Nullable<T> = T | null | undefined
            ${intrinsicsPrefix}function $ObjectInheritanceIntrinsic<T>(): T & (abstract new() => any);
        """.trimIndent().prependIndent(indent) + "\n"

        val declarationsDts = internalNamespace + declarations.joinTypeScriptFragments().raw

        val namespaceName = makeValidES5Identifier(name, withHash = false)

        return when (moduleKind) {
            ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
            ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> declarationsDts
            ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
        }
    }

    public fun generateTypeScriptFragment(declarations: List<ExportedDeclaration>): TypeScriptFragment {
        return TypeScriptFragment(declarations.toTypeScript())
    }

    private val ExportedDeclaration.isDefaultExport: Boolean
        get() = attributes.contains(ExportedAttribute.DefaultExport)

    private fun List<ExportedDeclaration>.toTypeScript(): String {
        return joinToString("\n") {
            it.toTypeScript(
                indent = indent,
                prefix = it.topLevelPrefix
            )
        }
    }

    private fun List<ExportedDeclaration>.toTypeScript(indent: String): String =
        joinToString("") { it.toTypeScript(indent) + "\n" }

    private fun ExportedDeclaration.toTypeScript(indent: String, prefix: String = ""): String =
        attributes.toTypeScript(indent) + indent + when (this) {
            is ErrorDeclaration -> generateTypeScriptString()
            is ExportedConstructor -> generateTypeScriptString(indent)
            is ExportedConstructSignature -> generateTypeScriptString(indent)
            is ExportedNamespace -> generateTypeScriptString(indent, prefix)
            is ExportedFunction -> generateTypeScriptString(indent, prefix)
            is ExportedRegularClass -> generateTypeScriptString(indent, prefix)
            is ExportedField -> generateTypeScriptString(indent, prefix)
            is ExportedPropertyAccessor -> generateTypeScriptString(indent, prefix)
            is ExportedObject -> generateTypeScriptString(indent, prefix)
        }

    private fun Iterable<ExportedAttribute>.toTypeScript(indent: String): String {
        return joinToString("\n") { it.toTypeScript(indent) }
            .run { if (isNotEmpty()) plus("\n") else this }
    }

    private fun ExportedDeclaration.generateDefaultExportIfNeed(name: String, indent: String): String {
        return if (moduleKind == ModuleKind.ES && isDefaultExport) {
            "\n$indent$defaultExportPrefix$name;"
        } else {
            ""
        }
    }

    private fun ExportedAttribute.toTypeScript(indent: String): String {
        return when (this) {
            is ExportedAttribute.DeprecatedAttribute -> indent + tsDeprecated(message)
            is ExportedAttribute.DefaultExport -> ""
        }
    }

    private fun ErrorDeclaration.generateTypeScriptString(): String {
        return "/* ErrorDeclaration: $message */"
    }

    private fun ExportedNamespace.generateTypeScriptString(indent: String, prefix: String): String {
        return "${prefix.takeIf { !isPrivate } ?: "declare "}namespace $name {\n" + declarations.toTypeScript("$indent    ") + "$indent}"
    }

    private fun ExportedConstructor.generateTypeScriptString(indent: String): String {
        return "${visibility.keyword}constructor(${parameters.generateTypeScriptString(indent)});"
    }

    private fun ExportedConstructSignature.generateTypeScriptString(indent: String): String {
        return "new${renderTypeParameters(typeParameters)}(${parameters.generateTypeScriptString(indent)}): ${returnType.toTypeScript(indent)};"
    }

    private val ExportedMember.propertyMemberName: String
        get() = when (val propertyName = name) {
            is ExportedMemberName.SymbolReference -> "[${propertyName.value}]"
            is ExportedMemberName.Identifier if !propertyName.value.isValidES5Identifier() -> "\"${propertyName.value}\""
            else -> propertyName.value
        }

    private fun ExportedField.generateTypeScriptString(indent: String, prefix: String): String {
        val extraIndent = "$indent    "
        val optional = if (isOptional) "?" else ""
        val memberName = propertyMemberName

        val typeToTypeScript = type.toTypeScript(if (!isMember && isEsModules && isObjectGetter) extraIndent else indent)

        return when {
            isMember -> {
                val static = if (isStatic) "static " else ""
                val abstract = if (isAbstract) "abstract " else ""
                val visibility = if (isProtected) "protected " else ""

                val readonly = if (!mutable) "readonly " else ""
                "$prefix$visibility$static$abstract$readonly$memberName$optional: $typeToTypeScript;"
            }
            memberName != name.value -> ""
            else -> {
                val keyword = if (mutable) "let " else "const "
                "$prefix$keyword$memberName$optional: $typeToTypeScript;"
            }
        }
    }

    private fun <T> T.generateTypeScriptString(indent: String, prefix: String): String
            where T : ExportedDeclaration,
                  T : ExportedPropertyAccessor =
        buildString {
            append(prefix)
            if (isProtected) {
                append("protected ")
            }
            if (isStatic) {
                append("static ")
            }
            if (isAbstract) {
                append("abstract ")
            }
            when (this@generateTypeScriptString) {
                is ExportedPropertyGetter -> {
                    append("get ")
                    append(this@generateTypeScriptString.propertyMemberName)
                    append("(): ")
                    append(this@generateTypeScriptString.type.toTypeScript(indent))
                }
                is ExportedPropertySetter -> {
                    append("set ")
                    append(this@generateTypeScriptString.propertyMemberName)
                    append("(value: ")
                    append(this@generateTypeScriptString.type.toTypeScript(indent))
                    append(")")
                }
            }
            append(";")
        }

    private fun renderTypeParameters(typeParameters: List<ExportedTypeParameter>): String = if (typeParameters.isNotEmpty()) {
        typeParameters.joinToString(", ", "<", ">") { tp ->
            tp.constraint?.let {
                "${tp.name} extends ${it.toTypeScript(indent, isInCommentContext = false)}"
            } ?: tp.name
        }
    } else {
        ""
    }

    private fun ExportedFunction.generateTypeScriptString(indent: String, prefix: String): String {
        val visibility = if (isProtected) "protected " else ""

        val keyword: String = when {
            isMember -> when {
                isStatic -> "static "
                isAbstract -> "abstract "
                else -> ""
            }

            else -> "function "
        }

        val renderedParameters = parameters.generateTypeScriptString(indent)
        val renderedTypeParameters = renderTypeParameters(typeParameters)

        val renderedReturnType = returnType.toTypeScript(indent)
        val containsUnresolvedChar = when (val exportedName = name) {
            is ExportedMemberName.Identifier -> !exportedName.value.isValidES5Identifier()
            is ExportedMemberName.SymbolReference -> true
        }

        val escapedName = when (val exportedName = name) {
            is ExportedMemberName.SymbolReference -> "[${exportedName.value}]"
            is ExportedMemberName.Identifier -> when {
                isMember && !exportedName.value.isValidES5Identifier() -> "\"${exportedName.value}\""
                else -> exportedName.value
            }
        }

        return if (!isMember && containsUnresolvedChar) {
            ""
        } else {
            "$prefix$visibility$keyword$escapedName$renderedTypeParameters($renderedParameters): $renderedReturnType;${
                generateDefaultExportIfNeed(
                    escapedName,
                    indent
                )
            }"
        }
    }

    private fun ExportedObject.generateTypeScriptString(indent: String, prefix: String): String {
        val shouldGenerateObjectWithGetInstance = isEsModules && !isExternal && isTopLevel
        val constructorTypeReference =
            if (shouldGenerateObjectWithGetInstance) MetadataConstructor else "$name.$Metadata.$MetadataConstructor"

        val substitutionOfObjectTypeToItsShapeClass = mapOf<ExportedType, ExportedType>(
            ExportedType.TypeOf(
                ExportedType.ClassType(
                    name,
                    emptyList(),
                    classId = originalClassId,
                )
            )
                    to ExportedType.ClassType(MetadataConstructor, emptyList())
        )

        val classContainingShape = ExportedRegularClass(
            name = MetadataConstructor,
            isInterface = false,
            isAbstract = true,
            isExternal = isExternal,
            requireMetadata = false,
            typeParameters = emptyList(),
            nestedClasses = emptyList(),
            superClasses = superClasses.map { it.replaceTypes(substitutionOfObjectTypeToItsShapeClass) },
            superInterfaces = superInterfaces,
            members = members + ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE),
            originalClassId = originalClassId,
        )

        val classContainingType = ExportedRegularClass(
            name = if (shouldGenerateObjectWithGetInstance) MetadataType else name,
            isInterface = false,
            isAbstract = true,
            isExternal = isExternal,
            requireMetadata = false,
            typeParameters = typeParameters,
            nestedClasses = nestedClasses,
            superClasses = listOfNotNull(
                ExportedType.ObjectsParentType(ExportedType.ClassType(constructorTypeReference, emptyList()))
            ),
            members = listOf(ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE)),
            originalClassId = originalClassId,
        )

        val extraClassWithGetInstanceMethod = runIf(shouldGenerateObjectWithGetInstance) {
            ExportedRegularClass(
                name = name,
                isInterface = false,
                isAbstract = true,
                isExternal = isExternal,
                requireMetadata = false,
                typeParameters = typeParameters,
                nestedClasses = emptyList(),
                superClasses = emptyList(),
                members = listOf(
                    ExportedField(
                        name = ExportedMemberName.Identifier(getInstance),
                        type = ExportedType.Function(
                            emptyList(),
                            ExportedType.TypeOf(ExportedType.ClassType("$name.$Metadata.$MetadataType", emptyList()))
                        ),
                        isMember = true,
                        mutable = false,
                        isStatic = true,
                    ),
                    ExportedConstructor(emptyList(), ExportedVisibility.PRIVATE),
                ),
                originalClassId = originalClassId,
            )
        }

        val metadataMembers =
            if (shouldGenerateObjectWithGetInstance) listOf(classContainingType, classContainingShape) else listOf(classContainingShape)

        val objectClass = (extraClassWithGetInstanceMethod ?: classContainingType).generateTypeScriptString(indent, prefix)
        val objectMetadata = ExportedNamespace(name, listOf(generateMetadataNamespace(metadataMembers))).toTypeScript(indent, prefix)

        return "$objectClass\n$objectMetadata${generateDefaultExportIfNeed(name, indent)}"
    }

    private fun ExportedRegularClass.generateTypeScriptString(indent: String, prefix: String): String {
        val keyword = if (isInterface) "interface" else "class"
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClasses.toExtendsClause(indent)
        val superInterfacesClause = superInterfaces.toImplementsClause(superInterfacesKeyword, indent)

        val (membersForNamespace, classMembers) = members.partition {
            it is ExportedNamespace || isInterface && it is ExportedMember && it.isStatic
        }

        val namespaceMembers = membersForNamespace.map {
            when (it) {
                is ExportedFunction -> it.copy(isMember = false)
                is ExportedField -> it.copy(isMember = false)
                else -> it
            }
        }

        val membersString = classMembers
            .joinToString("") { it.toTypeScript("$indent    ") + "\n" }

        // If there are no exported constructors, add a private constructor to disable default one
        val privateCtorString = if (!isInterface && !isAbstract && classMembers.none { it is ExportedConstructor }) {
            "$indent    private constructor();\n"
        } else {
            ""
        }

        val renderedTypeParameters = renderTypeParameters(typeParameters)

        val modifiers = if (isAbstract && !isInterface) "abstract " else ""

        val bodyString = privateCtorString + membersString + indent

        val metadataNamespace = listOfNotNull(runIf(requireMetadata) {
            val constructorProperty = ExportedField(
                name = ExportedMemberName.Identifier(MetadataConstructor),
                type = ExportedType.ConstructorType(
                    typeParameters,
                    ExportedType.ClassType(
                        name,
                        typeParameters.map(ExportedType::TypeParameterRef),
                        originalClassId,
                    )
                ),
                mutable = false,
                isQualified = true
            )
            generateMetadataNamespace(listOf(constructorProperty))
        })

        val realNestedDeclarations = metadataNamespace + namespaceMembers + nestedClasses

        val klassExport =
            "$prefix$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$bodyString}${
                generateDefaultExportIfNeed(
                    name,
                    indent
                )
            }"

        val staticsExport =
            if (realNestedDeclarations.isNotEmpty()) "\n" + ExportedNamespace(name, realNestedDeclarations).toTypeScript(indent, prefix) else ""

        return if (name.isValidES5Identifier()) klassExport + staticsExport else ""
    }

    private fun List<ExportedType>.toExtendsClause(indent: String): String {
        if (isEmpty()) return ""

        val implicitlyExportedClasses = filterIsInstance<ExportedType.ImplicitlyExportedType>()
        val implicitlyExportedClassesString = implicitlyExportedClasses.joinToString(", ") { it.toTypeScript(indent, true) }

        return if (implicitlyExportedClasses.count() == count()) {
            " /* extends $implicitlyExportedClassesString */"
        } else {
            val originallyDefinedSuperClass = implicitlyExportedClassesString.takeIf { it.isNotEmpty() }?.let { "/* $it */ " }.orEmpty()
            val transitivelyDefinedSuperClass = when (val parentType = single { it !is ExportedType.ImplicitlyExportedType }) {
                is ExportedType.ClassType -> ExportedType.ClassType(
                    "${parentType.name}.$Metadata.$MetadataConstructor",
                    parentType.arguments,
                    parentType.classId,
                )
                else -> parentType
            }.toTypeScript(indent, false)

            " extends $originallyDefinedSuperClass$transitivelyDefinedSuperClass"
        }
    }

    private fun List<ExportedType>.toImplementsClause(superInterfacesKeyword: String, indent: String): String {
        val (exportedInterfaces, nonExportedInterfaces) = partition { it !is ExportedType.ImplicitlyExportedType }
        val listOfNonExportedInterfaces = nonExportedInterfaces.joinToString(", ") {
            (it as ExportedType.ImplicitlyExportedType).type.toTypeScript(indent, true)
        }
        return when {
            exportedInterfaces.isEmpty() && nonExportedInterfaces.isNotEmpty() ->
                " /* $superInterfacesKeyword $listOfNonExportedInterfaces */"

            exportedInterfaces.isNotEmpty() -> {
                val nonExportedInterfacesTsString = if (nonExportedInterfaces.isNotEmpty()) "/*, $listOfNonExportedInterfaces */" else ""
                " $superInterfacesKeyword " + exportedInterfaces.joinToString(", ") { it.toTypeScript(indent) } + nonExportedInterfacesTsString
            }

            else -> ""
        }
    }

    private fun List<ExportedParameter>.generateTypeScriptString(indent: String): String {
        var couldBeOptional = true
        return asReversed()
            .mapIndexed { index, parameter ->
                if (!parameter.hasDefaultValue) couldBeOptional = false
                parameter.toTypeScript(indent, size - index - 1, couldBeOptional)
            }
            .asReversed()
            .joinToString()
    }

    private fun ExportedParameter.toTypeScript(indent: String, index: Int, couldBeOptional: Boolean): String {
        val name = name?.let { makeValidES5Identifier(it, withHash = false) } ?: "p$index"
        val type = if (hasDefaultValue && !couldBeOptional) {
            ExportedType.UnionType(type, ExportedType.Primitive.Undefined)
        } else type
        val questionMark = if (hasDefaultValue && couldBeOptional) "?" else ""
        return "$name$questionMark: ${type.toTypeScript(indent)}"
    }

    private fun ExportedType.toTypeScript(indent: String, isInCommentContext: Boolean = false): String = when (this) {
        is ExportedType.Primitive -> typescript
        is ExportedType.Array -> "Array<${elementType.toTypeScript(indent, isInCommentContext)}>"
        is ExportedType.ObjectsParentType -> "$ObjectInheritanceIntrinsic<${constructor.toTypeScript(indent, isInCommentContext)}>()"

        is ExportedType.Function ->
            "(" + parameters.generateTypeScriptString(indent) + ") => " + returnType.toTypeScript(indent, isInCommentContext)

        is ExportedType.ConstructorType ->
            "abstract new " + renderTypeParameters(typeParameters) + "() => ${returnType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ClassType -> {
            name + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript(indent, isInCommentContext) }}>" else ""
        }


        is ExportedType.TypeOf ->
            "typeof ${classType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ErrorType -> if (isInCommentContext) comment else "any /*$comment*/"
        is ExportedType.Nullable -> "$Nullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.NonNullable -> "$NonNullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.InlineInterfaceType -> {
            members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
        }

        is ExportedType.InlineArrayType -> {
            elements.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.toTypeScript(indent, isInCommentContext) }
        }

        is ExportedType.IntersectionType -> {
            lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.UnionType -> {
            lhs.toTypeScript(indent) + " | " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.LiteralType.StringLiteralType -> "\"$value\""
        is ExportedType.LiteralType.NumberLiteralType, is ExportedType.LiteralType.BooleanLiteralType -> value.toString()
        is ExportedType.ImplicitlyExportedType -> {
            val typeString = type.toTypeScript("", true)
            if (isInCommentContext) {
                typeString
            } else {
                val superTypeString = exportedSupertype.toTypeScript(indent)
                superTypeString.let { if (exportedSupertype is ExportedType.IntersectionType) "($it)" else it } + "/* $typeString */"
            }
        }

        is ExportedType.PropertyType -> "${container.toTypeScript(indent, isInCommentContext)}[${
            propertyName.toTypeScript(
                indent,
                isInCommentContext
            )
        }]"

        is ExportedType.TypeParameterRef -> typeParameter.name
    }

    private fun generateMetadataNamespace(members: List<ExportedDeclaration>): ExportedNamespace =
        ExportedNamespace(Metadata, members)
            .apply { attributes += ExportedAttribute.DeprecatedAttribute("$Metadata is used for internal purposes, please don't use it in your code, because it can be removed at any moment") }

    private fun tsDeprecated(message: String): String {
        return "/** @deprecated $message */"
    }
}
