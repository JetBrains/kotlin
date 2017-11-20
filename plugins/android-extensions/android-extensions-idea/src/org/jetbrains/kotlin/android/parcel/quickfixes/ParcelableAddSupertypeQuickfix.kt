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

import com.android.SdkConstants.CLASS_PARCELABLE
import org.jetbrains.kotlin.psi.*

class ParcelableAddSupertypeQuickfix(clazz: KtClassOrObject) : AbstractParcelableQuickFix<KtClassOrObject>(clazz) {
    object Factory : AbstractFactory({ findElement<KtClassOrObject>()?.let(::ParcelableAddSupertypeQuickfix) })
    override fun getText() = "Add ''Parcelable'' supertype"

    override fun invoke(ktPsiFactory: KtPsiFactory, element: KtClassOrObject) {
        element.addSuperTypeListEntry(ktPsiFactory.createSuperTypeEntry(CLASS_PARCELABLE)).shortenReferences()
    }
}