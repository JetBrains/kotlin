/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

object CirClassRecursionMarker : CirClass, CirRecursionMarker {
    override val annotations get() = unsupported()
    override val name get() = unsupported()
    override val typeParameters get() = unsupported()
    override val visibility get() = unsupported()
    override val modality get() = unsupported()
    override val kind get() = unsupported()
    override var companion: CirName?
        get() = unsupported()
        set(_) = unsupported()
    override val isCompanion get() = unsupported()
    override val isData get() = unsupported()
    override val isValue get() = unsupported()
    override val isInner get() = unsupported()
    override val hasEnumEntries: Boolean get() = unsupported()
    override var supertypes: List<CirType>
        get() = unsupported()
        set(_) = unsupported()
}

object CirTypeAliasRecursionMarker : CirTypeAlias, CirRecursionMarker {
    override val underlyingType: CirClassOrTypeAliasType get() = unsupported()
    override val expandedType: CirClassType get() = unsupported()
    override val annotations get() = unsupported()
    override val name get() = unsupported()
    override val typeParameters get() = unsupported()
    override val visibility get() = unsupported()
    override val isLiftedUp: Boolean get() = unsupported()
}
