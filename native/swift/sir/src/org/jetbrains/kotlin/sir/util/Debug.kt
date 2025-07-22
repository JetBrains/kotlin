/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*

val SirElement.debugString: String
    get() {
        return when (this) {
            is SirType -> "SirType(${render})"
            is SirVariable -> "var ${swiftFqName} { get${" set".takeIf { setter != null }} }"
            is SirGetter -> "${parent.swiftFqNameOrNull ?: "?"}.get"
            is SirSetter -> "${parent.swiftFqNameOrNull ?: "?"}.set"
            is SirInit -> "${swiftParentNamePrefix}.init${"?".takeIf { isFailable } ?: ""}(${renderParameters()})${renderEffects()}"
            is SirFunction -> "func ${swiftFqName}(${renderParameters()})${renderEffects()}"
            is SirExtension -> "extension ${extendedType.render}${renderProtocols.prefixIfNotEmpty(": ")}"
            is SirClass -> "class ${swiftFqName}${listOfNotNull(superClass?.render, renderProtocols.takeIf { it.isNotEmpty() }).joinToString().prefixIfNotEmpty(": ")}"
            is SirProtocol -> "protocol ${swiftFqName}${renderProtocols.prefixIfNotEmpty(": ")}"
            is SirEnum -> "enum ${swiftFqName}" // TODO: Render protocols
            is SirEnumCase -> "case ${swiftFqName}"
            is SirStruct -> "struct ${swiftFqName}" // TODO: Render protocols
            is SirTypealias -> "typealias ${swiftFqName} = ${type.render}"
            is SirModule -> "module ${swiftFqNameOrNull ?: "?"}"
            is SirSubscript -> "subscript(${renderParameters()}) { get${" set".takeIf { setter != null }} }"
        }
    }

private fun SirCallable.renderParameters(): String = allParameters.joinToString { "${it.argumentName ?: "_"} ${it.parameterName ?: "_"}: ${it.type.render}" }
private fun SirSubscript.renderParameters(): String = parameters.joinToString { "${it.argumentName ?: "_"} ${it.parameterName ?: "_"}: ${it.type.render}" }

private fun SirCallable.renderEffects(): String = listOfNotNull(
    errorType.takeUnless { it.isNever }?.let { "throws${it.takeUnless { it == SirType.any }?.let { "(${it.render})" } ?: ""}" }
).joinToString(separator = " ").prefixIfNotEmpty(" ")

private fun String.prefixIfNotEmpty(prefix: String): String = takeIf { it.isNotEmpty() }?.let { prefix + it }.orEmpty()

private val SirProtocolConformingDeclaration.renderProtocols: String get() = protocols.joinToString { it.swiftFqName }

private val SirType.render: String get() = when (this) {
        is SirOptionalType -> "${wrappedType.render}?"
        is SirArrayType -> "[${elementType.render}]"
        is SirDictionaryType -> "[${keyType.render} : ${valueType.render}]"
        is SirExistentialType -> protocols.takeIf { it.isNotEmpty() }?.joinToString(prefix = "any ", separator = " & ") { it.swiftFqName } ?: "Any"
        is SirNominalType -> "${typeDeclaration.swiftFqName}"
        is SirErrorType -> "<ERROR>"
        is SirUnsupportedType -> "<UNSUPPORTED>"
        is SirFunctionalType -> "(${parameterTypes.joinToString { it.render }}) -> ${returnType.render}"
    }
