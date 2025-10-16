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
            is ExportedProperty -> generateTypeScriptString(indent, prefix)
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
        return "new(${parameters.generateTypeScriptString(indent)}): ${returnType.toTypeScript(indent)};"
    }

    private fun ExportedProperty.generateTypeScriptString(indent: String, prefix: String): String {
        val extraIndent = "$indent    "
        val optional = if (isOptional) "?" else ""
        val containsUnresolvedChar = !name.isValidES5Identifier()
        val memberName = if (containsUnresolvedChar) "\"$name\"" else name

        val typeToTypeScript = type.toTypeScript(if (!isMember && isEsModules && isObjectGetter) extraIndent else indent)

        return if (isMember) {
            val static = if (isStatic) "static " else ""
            val abstract = if (isAbstract) "abstract " else ""
            val visibility = if (isProtected) "protected " else ""

            if (isField) {
                val readonly = if (!mutable) "readonly " else ""
                "$prefix$visibility$static$abstract$readonly$memberName$optional: $typeToTypeScript;"
            } else {
                val getter = "$prefix$visibility$static${abstract}get $memberName(): $typeToTypeScript;"
                val setter = runIf(mutable) { "\n$indent$prefix$visibility$static${abstract}set $memberName(value: $typeToTypeScript);" }
                getter + setter.orEmpty()
            }
        } else {
            when {
                containsUnresolvedChar -> ""
                isEsModules && !isQualified -> {
                    if (isObjectGetter) {
                        "${prefix}const $name: {\n${extraIndent}getInstance(): $typeToTypeScript;\n};"
                    } else {
                        val getter = "get(): $typeToTypeScript;"
                        val setter = runIf(mutable) { " set(value: $typeToTypeScript): void;" }
                        "${prefix}const $name: { $getter${setter.orEmpty()} };${generateDefaultExportIfNeed(name, indent)}"
                    }
                }

                else -> {
                    val keyword = if (mutable) "let " else "const "
                    "$prefix$keyword$memberName$optional: $typeToTypeScript;"
                }
            }
        }
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
        val renderedTypeParameters = if (typeParameters.isNotEmpty()) {
            "<" + typeParameters.joinToString(", ") { it.toTypeScript(indent) } + ">"
        } else {
            ""
        }

        val renderedReturnType = returnType.toTypeScript(indent)
        val containsUnresolvedChar = when (val exportedName = name) {
            is ExportedFunctionName.Identifier -> !exportedName.value.isValidES5Identifier()
            is ExportedFunctionName.WellKnownSymbol -> true
        }

        val escapedName = when (val exportedName = name) {
            is ExportedFunctionName.WellKnownSymbol -> "[Symbol.${exportedName.value}]"
            is ExportedFunctionName.Identifier -> when {
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
                    isObject = true,
                    isExternal = isExternal,
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
                    ExportedProperty(
                        name = getInstance,
                        type = ExportedType.Function(
                            emptyList(),
                            ExportedType.TypeOf(ExportedType.ClassType("$name.$Metadata.$MetadataType", emptyList()))
                        ),
                        isMember = true,
                        mutable = false,
                        isStatic = true,
                        isField = true
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
        val isInner = innerClassReference != null
        val keyword = if (isInterface) "interface" else "class"
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClasses.toExtendsClause(indent)
        val superInterfacesClause = superInterfaces.toImplementsClause(superInterfacesKeyword, indent)

        val (membersForNamespace, membersForClassItself) = members.partition { isInterface && it is ExportedFunction && it.isStatic }
        val namespaceMembers = membersForNamespace.map { (it as ExportedFunction).copy(isMember = false) }
        val classMembers = membersForClassItself.map {
            if (isInner && it is ExportedFunction && it.isStatic) {
                // Remove $outer argument from secondary constructors of inner classes
                it.copy(parameters = it.parameters.drop(1))
            } else {
                it
            }
        }

        val (innerClasses, nonInnerClasses) = nestedClasses.partition { it is ExportedRegularClass && it.innerClassReference != null }
        val innerClassesProperties = innerClasses.map { (it as ExportedRegularClass).toReadonlyProperty() }
        val membersString = (classMembers + innerClassesProperties)
            .joinToString("") { it.toTypeScript("$indent    ") + "\n" }

        // If there are no exported constructors, add a private constructor to disable default one
        val privateCtorString = if (!isInterface && !isAbstract && classMembers.none { it is ExportedConstructor }) {
            "$indent    private constructor();\n"
        } else {
            ""
        }

        val renderedTypeParameters = if (typeParameters.isNotEmpty()) {
            "<" + typeParameters.joinToString(", ") { it.toTypeScript(indent) } + ">"
        } else {
            ""
        }

        val modifiers = if (isAbstract && !isInterface) "abstract " else ""

        val bodyString = privateCtorString + membersString + indent

        val metadataNamespace = listOfNotNull(runIf(requireMetadata) {
            val constructorProperty = ExportedProperty(
                name = MetadataConstructor,
                type = ExportedType.ConstructorType(
                    typeParameters,
                    ExportedType.ClassType(
                        name,
                        typeParameters.map { it.copy(constraint = null) },
                        isObject = false,
                        isExternal,
                        originalClassId,
                    )
                ),
                mutable = false,
                isQualified = true
            )
            generateMetadataNamespace(listOf(constructorProperty))
        })

        val realNestedDeclarations = metadataNamespace + namespaceMembers + nonInnerClasses + innerClasses.map { it.withProtectedConstructors() }

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
                    parentType.isObject,
                    parentType.isExternal,
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

    private fun ExportedClass.withProtectedConstructors(): ExportedRegularClass {
        return (this as ExportedRegularClass).copy(members = members.map {
            if (it !is ExportedConstructor || it.isProtected) {
                it
            } else {
                it.copy(visibility = ExportedVisibility.PROTECTED)
            }
        })
    }

    private fun ExportedRegularClass.toReadonlyProperty(): ExportedProperty {
        val innerClassReference = innerClassReference ?: error("Can't create readonly property for non-inner class")
        val allPublicConstructors = members.asSequence()
            .filterIsInstance<ExportedConstructor>()
            .filterNot { it.isProtected }
            .map {
                ExportedConstructSignature(
                    parameters = it.parameters.drop(1),
                    returnType = ExportedType.TypeParameter(innerClassReference),
                )
            }
            .toList()

        val type = ExportedType.IntersectionType(
            ExportedType.InlineInterfaceType(allPublicConstructors),
            ExportedType.TypeOf(
                ExportedType.ClassType(
                    innerClassReference,
                    emptyList(),
                    isObject = false,
                    isExternal,
                    originalClassId,
                )
            )
        )

        return ExportedProperty(name = name, type = type, mutable = false, isMember = true)
    }

    private fun List<ExportedParameter>.generateTypeScriptString(indent: String): String {
        var couldBeOptional = true
        val parameters = foldRight(mutableListOf<String>()) { it, acc ->
            if (!it.hasDefaultValue) couldBeOptional = false
            acc.apply { add(0, it.toTypeScript(indent, couldBeOptional)) }
        }
        return parameters.joinToString(", ")
    }

    private fun ExportedParameter.toTypeScript(indent: String, couldBeOptional: Boolean): String {
        val name = makeValidES5Identifier(name, withHash = false)
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

        is ExportedType.Function -> "(" + parameterTypes
            .withIndex()
            .joinToString(", ") { (index, type) ->
                "p$index: ${type.toTypeScript(indent, isInCommentContext)}"
            } + ") => " + returnType.toTypeScript(indent, isInCommentContext)

        is ExportedType.ConstructorType ->
            "abstract new " + (if (typeParameters.isNotEmpty()) "<${
                typeParameters.joinToString(", ") {
                    it.toTypeScript(
                        indent,
                        isInCommentContext
                    )
                }
            }>" else "") + "() => ${returnType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ClassType -> {
            val classTypeReference = if (isObject && !isExternal && isEsModules) "$name.$Metadata.$MetadataType" else name
            classTypeReference + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript(indent, isInCommentContext) }}>" else ""
        }


        is ExportedType.TypeOf ->
            "typeof ${classType.toTypeScript(indent, isInCommentContext)}"

        is ExportedType.ErrorType -> if (isInCommentContext) comment else "any /*$comment*/"
        is ExportedType.Nullable -> "$Nullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.NonNullable -> "$NonNullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.InlineInterfaceType -> {
            members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
        }

        is ExportedType.IntersectionType -> {
            lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.UnionType -> {
            lhs.toTypeScript(indent) + " | " + rhs.toTypeScript(indent, isInCommentContext)
        }

        is ExportedType.LiteralType.StringLiteralType -> "\"$value\""
        is ExportedType.LiteralType.NumberLiteralType -> value.toString()
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

        is ExportedType.TypeParameter -> constraint?.let {
            "$name extends ${it.toTypeScript(indent, isInCommentContext)}"
        } ?: name
    }

    private fun generateMetadataNamespace(members: List<ExportedDeclaration>): ExportedNamespace =
        ExportedNamespace(Metadata, members)
            .apply { attributes += ExportedAttribute.DeprecatedAttribute("$Metadata is used for internal purposes, please don't use it in your code, because it can be removed at any moment") }

    private fun tsDeprecated(message: String): String {
        return "/** @deprecated $message */"
    }
}
