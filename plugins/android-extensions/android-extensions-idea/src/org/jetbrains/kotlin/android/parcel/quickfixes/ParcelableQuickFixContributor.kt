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

package org.jetbrains.kotlin.android.parcel.quickfixes

import org.jetbrains.kotlin.android.synthetic.diagnostic.ErrorsAndroid
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens

class ParcelableQuickFixContributor : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {
        quickFixes.register(ErrorsAndroid.PARCELABLE_CANT_BE_INNER_CLASS,
                            RemoveModifierFix.createRemoveModifierFromListOwnerFactory(KtTokens.INNER_KEYWORD, false))

        quickFixes.register(ErrorsAndroid.NO_PARCELABLE_SUPERTYPE, ParcelableAddSupertypeQuickfix.Factory)
        quickFixes.register(ErrorsAndroid.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR, ParcelableAddPrimaryConstructorQuickfix.Factory)
        quickFixes.register(ErrorsAndroid.PROPERTY_WONT_BE_SERIALIZED, ParcelableAddIgnoreOnParcelAnnotationQuickfix.Factory)

        quickFixes.register(ErrorsAndroid.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED, ParcelMigrateToParcelizeQuickFix.FactoryForWrite)
        quickFixes.register(ErrorsAndroid.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED, ParcelRemoveCustomWriteToParcel.Factory)

        quickFixes.register(ErrorsAndroid.CREATOR_DEFINITION_IS_NOT_ALLOWED, ParcelMigrateToParcelizeQuickFix.FactoryForCREATOR)
        quickFixes.register(ErrorsAndroid.CREATOR_DEFINITION_IS_NOT_ALLOWED, ParcelRemoveCustomCreatorProperty.Factory)

        quickFixes.register(ErrorsAndroid.REDUNDANT_TYPE_PARCELER, ParcelableRemoveDuplicatingTypeParcelerAnnotationQuickFix.Factory)
        quickFixes.register(ErrorsAndroid.CLASS_SHOULD_BE_PARCELIZE, AnnotateWithParcelizeQuickFix.Factory)
    }
}