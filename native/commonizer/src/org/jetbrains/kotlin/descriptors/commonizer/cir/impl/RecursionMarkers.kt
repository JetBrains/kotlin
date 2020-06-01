/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifier
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRecursionMarker
import org.jetbrains.kotlin.name.FqName

object CirClassRecursionMarker : CirClass, CirRecursionMarker {
    override val annotations get() = unsupported()
    override val name get() = unsupported()
    override val typeParameters get() = unsupported()
    override val visibility get() = unsupported()
    override val modality get() = unsupported()
    override val kind get() = unsupported()
    override var companion: FqName?
        get() = unsupported()
        set(_) = unsupported()
    override val isCompanion get() = unsupported()
    override val isData get() = unsupported()
    override val isInline get() = unsupported()
    override val isInner get() = unsupported()
    override val isExternal get() = unsupported()
    override val supertypes get() = unsupported()
}

object CirClassifierRecursionMarker : CirClassifier, CirRecursionMarker {
    override val annotations get() = unsupported()
    override val name get() = unsupported()
    override val typeParameters get() = unsupported()
    override val visibility get() = unsupported()
}
