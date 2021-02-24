/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers

class ValueParameterListCommonizer(classifiers: CirKnownClassifiers) : AbstractListCommonizer<CirValueParameter, CirValueParameter>(
    singleElementCommonizerFactory = { ValueParameterCommonizer(classifiers) }
) {
    fun overwriteNames(names: List<CirName>) {
        forEachSingleElementCommonizer { index, singleElementCommonizer ->
            (singleElementCommonizer as ValueParameterCommonizer).overwriteName(names[index])
        }
    }
}
