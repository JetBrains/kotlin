/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey

object RhizomedbPluginKey : GeneratedDeclarationKey() {
    override fun toString(): String {
        return "RhizomedbPlugin"
    }
}

data class RhizomedbAttributePluginKey(val kind: RhizomedbAttributeKind) : GeneratedDeclarationKey() {
    override fun toString(): String {
        return "RhizomedbPlugin: $kind"
    }
}