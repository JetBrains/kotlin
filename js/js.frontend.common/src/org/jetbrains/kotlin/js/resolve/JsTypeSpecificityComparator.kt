/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate

// Replacing the TypeSystemInferenceExtensionContextDelegate with
// TypeSystemInferenceExtensionContext here leads to clashed registrations
// (for example, when trying to run IrJsTextTestCaseGenerated).
class JsTypeSpecificityComparator(context: TypeSystemInferenceExtensionContextDelegate) :
    TypeSpecificityComparator by JsTypeSpecificityComparatorWithoutDelegate(context)

class JsTypeSpecificityComparatorWithoutDelegate(val context: TypeSystemInferenceExtensionContext) : TypeSpecificityComparator {
    private fun TypeSystemInferenceExtensionContext.checkOnlyDynamicFlexibleType(type: KotlinTypeMarker) {
        if (type.asFlexibleType() != null) {
            assert(type.isDynamic()) {
                "Unexpected flexible type in Js: $type"
            }
        }
    }

    override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean = with(context) {
        checkOnlyDynamicFlexibleType(specific)
        checkOnlyDynamicFlexibleType(general)

        return specific.isDynamic() && !general.isDynamic()
    }
}
