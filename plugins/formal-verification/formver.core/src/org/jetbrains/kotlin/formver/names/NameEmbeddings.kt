/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.formver.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

/* This file contains classes to mangle names present in the Kotlin source.
 *
 * Name components should be separated by dollar signs.
 * If there is a risk of collision, add a prefix.
 */


fun CallableId.embedScopeName(): NameScope =
    when (val id = this.classId) {
        null -> GlobalScope(packageName)
        else -> ClassScope(packageName, ClassKotlinName(id.relativeClassName))
    }

fun CallableId.embedScoped(name: KotlinName) = ScopedKotlinName(embedScopeName(), name)
fun CallableId.embedScopedWithType(type: TypeEmbedding, name: KotlinName) = ScopedKotlinName(embedScopeName(), name, type)

fun ClassId.embedName(): ScopedKotlinName = ScopedKotlinName(GlobalScope(packageFqName), ClassKotlinName(relativeClassName))
fun CallableId.embedGetterName(): ScopedKotlinName = embedScoped(GetterKotlinName(callableName))
fun CallableId.embedSetterName(): ScopedKotlinName = embedScoped(SetterKotlinName(callableName))
fun CallableId.embedExtensionGetterName(type: TypeEmbedding): ScopedKotlinName =
    embedScopedWithType(type, ExtensionGetterKotlinName(callableName))
fun CallableId.embedExtensionSetterName(type: TypeEmbedding): ScopedKotlinName =
    embedScopedWithType(type, ExtensionSetterKotlinName(callableName))
fun CallableId.embedMemberPropertyName(): ScopedKotlinName = embedScoped(MemberKotlinName(callableName))
fun CallableId.embedUnscopedPropertyName(): SimpleKotlinName = SimpleKotlinName(callableName)
fun CallableId.embedFunctionName(type: TypeEmbedding): ScopedKotlinName =
    embedScopedWithType(type, FunctionKotlinName(callableName))

fun CallableId.embedPropertyName(scope: Int): ScopedKotlinName = when {
    isLocal -> ScopedKotlinName(LocalScope(scope), SimpleKotlinName(callableName))
    className != null -> embedMemberPropertyName()
    else -> error("Name is neither local nor bound to a class; we do not know how to handle this.")
}

fun FirValueParameterSymbol.embedName(): ScopedKotlinName = ScopedKotlinName(ParameterScope, SimpleKotlinName(name))
fun FirPropertyAccessorSymbol.embedName(ctx: ProgramConversionContext): ScopedKotlinName = when (propertySymbol.isExtension) {
    true -> when {
        isGetter -> propertySymbol.callableId.embedExtensionGetterName(ctx.embedType(this))
        isSetter -> propertySymbol.callableId.embedExtensionSetterName(ctx.embedType(this))
        else -> error("An extension property must be a setter or a getter!")
    }
    false -> when {
        isGetter -> propertySymbol.callableId.embedGetterName()
        isSetter -> propertySymbol.callableId.embedSetterName()
        else -> error("A property accessor must be a setter or a getter!")
    }
}

fun FirConstructorSymbol.embedName(ctx: ProgramConversionContext): ScopedKotlinName =
    callableId.embedScopedWithType(ctx.embedType(this), ConstructorKotlinName)

fun FirFunctionSymbol<*>.embedName(ctx: ProgramConversionContext): ScopedKotlinName = when (this) {
    is FirPropertyAccessorSymbol -> embedName(ctx)
    is FirConstructorSymbol -> embedName(ctx)
    else -> callableId.embedFunctionName(ctx.embedType(this))
}
