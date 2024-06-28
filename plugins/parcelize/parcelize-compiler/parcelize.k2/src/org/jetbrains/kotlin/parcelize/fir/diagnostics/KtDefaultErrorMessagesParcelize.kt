/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.RENDER_CLASS_OR_OBJECT_QUOTED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.CLASS_SHOULD_BE_PARCELIZE
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.DEPRECATED_ANNOTATION
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.DEPRECATED_PARCELER
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.DUPLICATING_TYPE_PARCELERS
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.FORBIDDEN_DEPRECATED_ANNOTATION
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.NO_PARCELABLE_SUPERTYPE
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_CANT_BE_INNER_CLASS
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_CANT_BE_LOCAL_CLASS
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_DELEGATE_IS_NOT_ALLOWED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_SHOULD_BE_INSTANTIABLE
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_SHOULD_NOT_BE_ENUM_CLASS
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELABLE_TYPE_NOT_SUPPORTED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELER_SHOULD_BE_OBJECT
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PARCELER_TYPE_INCOMPATIBLE
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED
import org.jetbrains.kotlin.parcelize.fir.diagnostics.KtErrorsParcelize.REDUNDANT_TYPE_PARCELER

object KtDefaultErrorMessagesParcelize : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("Parcelize").also { map ->
        map.put(
            PARCELABLE_SHOULD_BE_CLASS,
            "'Parcelable' should be a class."
        )

        map.put(
            PARCELABLE_DELEGATE_IS_NOT_ALLOWED,
            "Delegating 'Parcelable' is not allowed."
        )

        map.put(
            PARCELABLE_SHOULD_NOT_BE_ENUM_CLASS,
            "'Parcelable' should not be a 'enum class'."
        )

        map.put(
            PARCELABLE_SHOULD_BE_INSTANTIABLE,
            "'Parcelable' should not be an 'abstract' class."
        )

        map.put(
            PARCELABLE_CANT_BE_INNER_CLASS,
            "'Parcelable' can't be an inner class."
        )

        map.put(
            PARCELABLE_CANT_BE_LOCAL_CLASS,
            "'Parcelable' can't be a local class."
        )

        map.put(
            NO_PARCELABLE_SUPERTYPE,
            "No 'Parcelable' supertype."
        )

        map.put(
            PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR,
            "'Parcelable' should have a primary constructor."
        )

        map.put(
            PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY,
            "The primary constructor is empty, no data will be serialized to 'Parcel'."
        )

        map.put(
            PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR,
            "'Parcelable' constructor parameter should be 'val' or 'var'."
        )

        map.put(
            PROPERTY_WONT_BE_SERIALIZED,
            "Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning."
        )

        map.put(
            OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED,
            "Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead."
        )

        map.put(
            CREATOR_DEFINITION_IS_NOT_ALLOWED,
            "'CREATOR' definition is not allowed. Use 'Parceler' companion object instead."
        )

        map.put(
            PARCELABLE_TYPE_NOT_SUPPORTED,
            "Type is not directly supported by 'Parcelize'. " +
                    "Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'."
        )

        map.put(
            PARCELER_SHOULD_BE_OBJECT,
            "Parceler should be an object."
        )

        map.put(
            PARCELER_TYPE_INCOMPATIBLE,
            "Parceler type {0} is incompatible with {1}.",
            RENDER_TYPE, RENDER_TYPE
        )

        map.put(
            DUPLICATING_TYPE_PARCELERS,
            "Duplicating 'TypeParceler' annotations."
        )

        map.put(
            REDUNDANT_TYPE_PARCELER,
            "This ''TypeParceler'' is already provided for {0}.",
            RENDER_CLASS_OR_OBJECT_QUOTED
        )

        map.put(
            CLASS_SHOULD_BE_PARCELIZE,
            "{0} should be annotated with ''@Parcelize''.",
            RENDER_CLASS_OR_OBJECT_QUOTED
        )

        map.put(
            INAPPLICABLE_IGNORED_ON_PARCEL,
            "'@IgnoredOnParcel' is only applicable to class properties."
        )

        map.put(
            INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY,
            "'@IgnoredOnParcel' is inapplicable to properties without default value declared in the primary constructor."
        )

        map.put(
            FORBIDDEN_DEPRECATED_ANNOTATION,
            "Parceler-related annotations from package 'kotlinx.android.parcel' are forbidden. Change package to 'kotlinx.parcelize'."
        )

        map.put(
            DEPRECATED_ANNOTATION,
            "Parcelize annotations from package 'kotlinx.android.parcel' are deprecated. Change package to 'kotlinx.parcelize'."
        )

        map.put(
            DEPRECATED_PARCELER,
            "'kotlinx.android.parcel.Parceler' is deprecated. Use 'kotlinx.parcelize.Parceler' instead."
        )
    }
}
