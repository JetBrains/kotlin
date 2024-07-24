/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.formver.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/* This file contains classes to mangle names present in the Kotlin source.
 *
 * Name components should be separated by dollar signs.
 * If there is a risk of collision, add a prefix.
 */

fun ClassId.embedLocalName(): ClassKotlinName = ClassKotlinName(relativeClassName)

fun ScopedKotlinNameBuilder.embedScope(id: CallableId) {
    packageScope(id.packageName)
    when (val classId = id.classId) {
        null -> globalScope()
        else -> classScope(classId.embedLocalName())
    }
}

fun ScopedKotlinNameBuilder.embedScope(id: ClassId) {
    packageScope(id.packageFqName)
    classScope(id.embedLocalName())
}

fun ClassId.embedName(): ScopedKotlinName = buildName {
    packageScope(packageFqName)
    globalScope()
    embedLocalName()
}

fun CallableId.embedExtensionGetterName(type: TypeEmbedding): ScopedKotlinName = buildName {
    embedScope(this@embedExtensionGetterName)
    ExtensionGetterKotlinName(callableName, type)
}

fun CallableId.embedExtensionSetterName(type: TypeEmbedding): ScopedKotlinName = buildName {
    embedScope(this@embedExtensionSetterName)
    ExtensionSetterKotlinName(callableName, type)
}

private fun CallableId.embedMemberPropertyNameBase(isPrivate: Boolean, withAction: (Name) -> KotlinName): ScopedKotlinName {
    val id = classId ?: error("Embedding non-member property $callableName as a member.")
    return buildName {
        embedScope(id)
        if (isPrivate) privateScope() else publicScope()
        withAction(callableName)
    }
}

fun CallableId.embedMemberPropertyName(isPrivate: Boolean) = embedMemberPropertyNameBase(isPrivate, ::PropertyKotlinName)
fun CallableId.embedMemberGetterName(isPrivate: Boolean) = embedMemberPropertyNameBase(isPrivate, ::GetterKotlinName)
fun CallableId.embedMemberSetterName(isPrivate: Boolean) = embedMemberPropertyNameBase(isPrivate, ::SetterKotlinName)
fun CallableId.embedMemberBackingFieldName(isPrivate: Boolean) = embedMemberPropertyNameBase(isPrivate, ::BackingFieldKotlinName)

fun CallableId.embedUnscopedPropertyName(): SimpleKotlinName = SimpleKotlinName(callableName)
fun CallableId.embedFunctionName(type: TypeEmbedding): ScopedKotlinName = buildName {
    embedScope(this@embedFunctionName)
    FunctionKotlinName(callableName, type)
}

fun Name.embedScopedLocalName(scope: Int) = buildName {
    localScope(scope)
    SimpleKotlinName(this@embedScopedLocalName)
}

fun Name.embedParameterName() = buildName {
    parameterScope()
    SimpleKotlinName(this@embedParameterName)
}

fun FirValueParameterSymbol.embedName(): ScopedKotlinName = name.embedParameterName()

fun FirPropertySymbol.embedGetterName(ctx: ProgramConversionContext): ScopedKotlinName = when (isExtension) {
    true -> callableId.embedExtensionGetterName(ctx.embedType(getterSymbol!!))
    false -> callableId.embedMemberGetterName(Visibilities.isPrivate(visibility))
}

fun FirPropertySymbol.embedSetterName(ctx: ProgramConversionContext): ScopedKotlinName = when (isExtension) {
    true -> callableId.embedExtensionSetterName(ctx.embedType(setterSymbol ?: error("Embedding setter of read-only extension property.")))
    false -> callableId.embedMemberSetterName(Visibilities.isPrivate(visibility))
}

fun FirPropertySymbol.embedMemberPropertyName() = callableId.embedMemberPropertyName(
    Visibilities.isPrivate(this.visibility)
)

fun FirConstructorSymbol.embedName(ctx: ProgramConversionContext): ScopedKotlinName = buildName {
    embedScope(callableId)
    ConstructorKotlinName(ctx.embedType(this@embedName))
}

fun FirFunctionSymbol<*>.embedName(ctx: ProgramConversionContext): ScopedKotlinName = when (this) {
    is FirPropertyAccessorSymbol -> if (isGetter) propertySymbol.embedGetterName(ctx) else propertySymbol.embedSetterName(ctx)
    is FirConstructorSymbol -> embedName(ctx)
    else -> callableId.embedFunctionName(ctx.embedType(this))
}
