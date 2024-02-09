/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirOrigin {
    sealed interface Synthetic : SirOrigin

    data class ExternallyDefined(val name: String) : Synthetic

    data class Namespace(val path: List<String>) : Synthetic

    sealed interface Foreign : SirOrigin {

        interface SourceCode : Foreign
    }

    /**
     * Value for nodes of unknown or non-viable origin
     * (e.g. objects created in/for tests)
     */
    data object Unknown : SirOrigin
}