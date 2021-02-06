/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

interface CirClass : CirClassifier, CirContainingClass {
    var companion: CirName? // null means no companion object
    val isCompanion: Boolean
    val isInline: Boolean
    val isInner: Boolean
    val isExternal: Boolean
    val supertypes: Collection<CirType>

    fun setSupertypes(supertypes: Collection<CirType>)
}
