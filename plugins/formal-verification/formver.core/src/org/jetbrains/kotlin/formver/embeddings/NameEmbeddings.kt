/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.conversion.ProgramConversionContext
import org.jetbrains.kotlin.formver.viper.MangledName
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
        else -> ClassScope(packageName, id.embedName())
    }

fun CallableId.embedScoped(f: CallableId.() -> KotlinName) = ScopedKotlinName(embedScopeName(), f())
fun CallableId.embedScopedWithType(type: TypeEmbedding, f: CallableId.() -> KotlinName) = ScopedKotlinName(embedScopeName(), f(), type)

fun ClassId.embedName(): ScopedKotlinName = ScopedKotlinName(GlobalScope(packageFqName), ClassKotlinName(shortClassName))
fun CallableId.embedGetterName(): ScopedKotlinName = embedScoped { GetterKotlinName(callableName) }
fun CallableId.embedSetterName(): ScopedKotlinName = embedScoped { SetterKotlinName(callableName) }
fun CallableId.embedMemberPropertyName(): MangledName = embedScoped { MemberKotlinName(callableName) }

fun CallableId.embedPropertyName(scope: Int): MangledName = when {
    isLocal -> ScopedKotlinName(LocalScope(scope), SimpleKotlinName(callableName))
    className != null -> embedMemberPropertyName()
    else -> throw IllegalStateException("Name is neither local nor bound to a class; we do not know how to handle this.")
}

fun FirValueParameterSymbol.embedName(): ScopedKotlinName = ScopedKotlinName(ParameterScope, SimpleKotlinName(name))
fun FirPropertyAccessorSymbol.embedName(): MangledName = when {
    isGetter -> callableId.embedGetterName()
    isSetter -> callableId.embedSetterName()
    else -> throw IllegalStateException("A property accessor must be a setter or a getter!")
}

fun FirConstructorSymbol.embedName(ctx: ProgramConversionContext): MangledName =
    callableId.embedScopedWithType(ctx.embedType(this)) { ConstructorKotlinName }

fun FirFunctionSymbol<*>.embedName(ctx: ProgramConversionContext): MangledName = when (this) {
    is FirPropertyAccessorSymbol -> embedName()
    is FirConstructorSymbol -> embedName(ctx)
    else -> callableId.embedScopedWithType(ctx.embedType(this)) { FunctionKotlinName(callableId.callableName) }
}

