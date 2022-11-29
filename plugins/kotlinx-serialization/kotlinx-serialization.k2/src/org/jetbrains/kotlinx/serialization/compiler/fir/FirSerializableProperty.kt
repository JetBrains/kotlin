/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.fir.services.analyzeSpecialSerializers
import org.jetbrains.kotlinx.serialization.compiler.resolve.ISerializableProperties
import org.jetbrains.kotlinx.serialization.compiler.resolve.ISerializableProperty

class FirSerializableProperty(
    session: FirSession,
    val propertySymbol: FirPropertySymbol,
    override val isConstructorParameterWithDefault: Boolean,
    declaresDefaultValue: Boolean
) : ISerializableProperty {
    override val name: String = propertySymbol.serialNameValue ?: propertySymbol.name.asString()

    override val originalDescriptorName: Name
        get() = propertySymbol.name

    override val optional: Boolean = !propertySymbol.serialRequired && declaresDefaultValue

    override val transient: Boolean = propertySymbol.hasSerialTransient || !propertySymbol.hasBackingField

    val serializableWith: ConeKotlinType? = propertySymbol.serializableWith
        ?: analyzeSpecialSerializers(session, propertySymbol.resolvedAnnotationsWithArguments)?.defaultType()
}

class FirSerializableProperties(
    override val serializableProperties: List<FirSerializableProperty>,
    override val isExternallySerializable: Boolean,
    override val serializableConstructorProperties: List<FirSerializableProperty>,
    override val serializableStandaloneProperties: List<FirSerializableProperty>,
) : ISerializableProperties<FirSerializableProperty>
