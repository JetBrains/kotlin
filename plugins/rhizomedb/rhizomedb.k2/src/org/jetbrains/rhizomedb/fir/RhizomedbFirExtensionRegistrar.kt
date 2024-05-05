/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.rhizomedb.fir.checkers.RhizomedbFirCheckersComponent
import org.jetbrains.rhizomedb.fir.services.RhizomedbEntityPredicateMatcher
import org.jetbrains.rhizomedb.fir.services.RhizomedbFirAttributesProvider

class RhizomedbFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::RhizomedbFirResolveExtension
        +::RhizomedbFirSupertypesExtension
        +::RhizomedbFirCheckersComponent

        // services
        +::RhizomedbFirAttributesProvider
        +::RhizomedbEntityPredicateMatcher
    }
}
