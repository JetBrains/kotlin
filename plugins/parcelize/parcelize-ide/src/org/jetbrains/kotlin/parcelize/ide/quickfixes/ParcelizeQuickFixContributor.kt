/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.parcelize.ide.quickfixes

import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize

class ParcelizeQuickFixContributor : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {
        quickFixes.register(
            ErrorsParcelize.PARCELABLE_CANT_BE_INNER_CLASS,
            RemoveModifierFix.createRemoveModifierFromListOwnerPsiBasedFactory(KtTokens.INNER_KEYWORD, false)
        )

        quickFixes.register(ErrorsParcelize.NO_PARCELABLE_SUPERTYPE, ParcelizeAddSupertypeQuickFix.Factory)
        quickFixes.register(ErrorsParcelize.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR, ParcelizeAddPrimaryConstructorQuickFix.Factory)
        quickFixes.register(ErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED, ParcelizeAddIgnoreOnParcelAnnotationQuickFix.Factory)

        quickFixes.register(ErrorsParcelize.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED, ParcelMigrateToParcelizeQuickFix.FactoryForWrite)
        quickFixes.register(ErrorsParcelize.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED, ParcelRemoveCustomWriteToParcel.Factory)

        quickFixes.register(ErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, ParcelMigrateToParcelizeQuickFix.FactoryForCREATOR)
        quickFixes.register(ErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, ParcelRemoveCustomCreatorProperty.Factory)

        quickFixes.register(ErrorsParcelize.REDUNDANT_TYPE_PARCELER, ParcelizeRemoveDuplicatingTypeParcelerAnnotationQuickFix.Factory)
        quickFixes.register(ErrorsParcelize.CLASS_SHOULD_BE_PARCELIZE, AnnotateWithParcelizeQuickFix.Factory)
    }
}