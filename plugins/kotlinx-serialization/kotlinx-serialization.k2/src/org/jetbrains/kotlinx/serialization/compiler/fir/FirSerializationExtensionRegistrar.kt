/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.FirSerializationCheckersComponent
import org.jetbrains.kotlinx.serialization.compiler.fir.services.ContextualSerializersProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.DependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.FirSerializablePropertiesProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.FirVersionReader

class FirSerializationExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SerializationFirResolveExtension
        +::SerializationFirSupertypesExtension
        +::FirSerializationCheckersComponent
        @OptIn(FirExtensionApiInternals::class)
        +::FirSerializationMetadataSerializerPlugin

        // services
        +::DependencySerializationInfoProvider
        +::FirSerializablePropertiesProvider
        +::FirVersionReader
        +::ContextualSerializersProvider
    }
}
