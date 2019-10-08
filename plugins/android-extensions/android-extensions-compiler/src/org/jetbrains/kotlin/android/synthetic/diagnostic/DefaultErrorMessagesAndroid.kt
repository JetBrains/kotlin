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

package org.jetbrains.kotlin.android.synthetic.diagnostic

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_CLASS_OR_OBJECT
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE_WITH_ANNOTATIONS

object DefaultErrorMessagesAndroid : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("Android")
    override fun getMap() = MAP

    init {
        MAP.put(ErrorsAndroid.SYNTHETIC_INVALID_WIDGET_TYPE,
                "Widget has an invalid type ''{0}''. Please specify the fully qualified widget class name in XML",
                Renderers.TO_STRING)

        MAP.put(ErrorsAndroid.SYNTHETIC_UNRESOLVED_WIDGET_TYPE,
                "Widget has an unresolved type ''{0}'', and thus it was upcasted to ''android.view.View''",
                Renderers.TO_STRING)

        MAP.put(ErrorsAndroid.SYNTHETIC_DEPRECATED_PACKAGE,
                "Use properties from the build variant packages")

        MAP.put(ErrorsAndroid.UNSAFE_CALL_ON_PARTIALLY_DEFINED_RESOURCE,
                "Potential NullPointerException. The resource is missing in some of layout versions")

        MAP.put(ErrorsAndroid.PARCELABLE_SHOULD_BE_CLASS,
                "'Parcelable' should be a class")

        MAP.put(ErrorsAndroid.PARCELABLE_DELEGATE_IS_NOT_ALLOWED,
                "Delegating 'Parcelable' is not allowed")

        MAP.put(ErrorsAndroid.PARCELABLE_SHOULD_NOT_BE_ENUM_CLASS,
                "'Parcelable' should not be a 'enum class'")

        MAP.put(ErrorsAndroid.PARCELABLE_SHOULD_BE_INSTANTIABLE,
                "'Parcelable' should not be a 'sealed' or 'abstract' class")

        MAP.put(ErrorsAndroid.PARCELABLE_CANT_BE_INNER_CLASS,
                "'Parcelable' can't be an inner class")

        MAP.put(ErrorsAndroid.PARCELABLE_CANT_BE_LOCAL_CLASS,
                "'Parcelable' can't be a local class")

        MAP.put(ErrorsAndroid.NO_PARCELABLE_SUPERTYPE,
                "No 'Parcelable' supertype")

        MAP.put(ErrorsAndroid.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR,
                "'Parcelable' should have a primary constructor")

        MAP.put(ErrorsAndroid.PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY,
                "The primary constructor is empty, no data will be serialized to 'Parcel'")

        MAP.put(ErrorsAndroid.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR,
                "'Parcelable' constructor parameter should be 'val' or 'var'")

        MAP.put(ErrorsAndroid.PROPERTY_WONT_BE_SERIALIZED,
                "Property would not be serialized into a 'Parcel'. Add '@IgnoredOnParcel' annotation to remove the warning")

        MAP.put(ErrorsAndroid.OVERRIDING_WRITE_TO_PARCEL_IS_NOT_ALLOWED,
                "Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead")

        MAP.put(ErrorsAndroid.CREATOR_DEFINITION_IS_NOT_ALLOWED,
                "'CREATOR' definition is not allowed. Use 'Parceler' companion object instead")

        MAP.put(ErrorsAndroid.PARCELABLE_TYPE_NOT_SUPPORTED,
                "Type is not directly supported by 'Parcelize'. " +
                "Annotate the parameter type with '@RawValue' if you want it to be serialized using 'writeValue()'")

        MAP.put(ErrorsAndroid.PARCELER_SHOULD_BE_OBJECT,
                "Parceler should be an object")

        MAP.put(ErrorsAndroid.PARCELER_TYPE_INCOMPATIBLE,
                "Parceler type {0} is incompatible with {1}",
                RENDER_TYPE_WITH_ANNOTATIONS, RENDER_TYPE_WITH_ANNOTATIONS)

        MAP.put(ErrorsAndroid.DUPLICATING_TYPE_PARCELERS,
                "Duplicating ''TypeParceler'' annotations")

        MAP.put(ErrorsAndroid.REDUNDANT_TYPE_PARCELER,
                "This ''TypeParceler'' is already provided for {0}",
                RENDER_CLASS_OR_OBJECT)

        MAP.put(ErrorsAndroid.CLASS_SHOULD_BE_PARCELIZE,
                "{0} should be annotated with ''@Parcelize''",
                RENDER_CLASS_OR_OBJECT)

        MAP.put(ErrorsAndroid.INAPPLICABLE_IGNORED_ON_PARCEL,
                "'@IgnoredOnParcel' is only applicable to class properties")

        MAP.put(ErrorsAndroid.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY,
                "'@IgnoredOnParcel' is inapplicable to properties declared in the primary constructor")
    }
}
