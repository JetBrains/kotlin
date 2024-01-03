/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.serialization.FirElementAwareStringTable
import org.jetbrains.kotlin.fir.serialization.FirMetadataSerializerPlugin
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlinx.serialization.compiler.fir.services.serializablePropertiesProvider

@OptIn(FirExtensionApiInternals::class)
class FirSerializationMetadataSerializerPlugin(session: FirSession) : FirMetadataSerializerPlugin(session) {
    override fun registerProtoExtensions(
        symbol: FirRegularClassSymbol,
        stringTable: FirElementAwareStringTable,
        protoRegistrar: ProtoRegistrar,
    ) {
        if (!symbol.needSaveProgramOrder) return
        val properties = session.serializablePropertiesProvider.getSerializablePropertiesForClass(symbol)
        val propertyNames = properties.serializableProperties.map { stringTable.getStringIndex(it.propertySymbol.name.asString()) }
        protoRegistrar.setExtension(SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder, propertyNames)
    }

    private val FirRegularClassSymbol.needSaveProgramOrder: Boolean
        get() = (modality == Modality.OPEN || modality == Modality.ABSTRACT) && with(session) { isInternalSerializable }
}
