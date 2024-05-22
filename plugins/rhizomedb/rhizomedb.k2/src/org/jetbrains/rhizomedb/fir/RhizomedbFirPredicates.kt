/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

object RhizomedbFirPredicates {
    internal val annotatedWithEntityType = DeclarationPredicate.create {
        annotated(RhizomedbAnnotations.generatedEntityTypeFqName)
    }

    internal val annotatedWithAttribute = DeclarationPredicate.create {
        annotated(RhizomedbAnnotations.valueAttributeFqName) or
                annotated(RhizomedbAnnotations.transientAttributeFqName) or
                annotated(RhizomedbAnnotations.referenceAttributeFqName)
    }

    internal val annotatedWithMany = DeclarationPredicate.create {
        annotated(RhizomedbAnnotations.manyFqName)
    }
}
