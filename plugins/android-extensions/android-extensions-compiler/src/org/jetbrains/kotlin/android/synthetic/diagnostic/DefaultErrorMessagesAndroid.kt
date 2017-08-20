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

class DefaultErrorMessagesAndroid : DefaultErrorMessages.Extension {

    private companion object {
        val MAP = DiagnosticFactoryToRendererMap("Android")

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

            MAP.put(ErrorsAndroid.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR,
                    "'Parcelable' constructor parameter should be 'val' or 'var'")

            MAP.put(ErrorsAndroid.PROPERTY_WONT_BE_SERIALIZED,
                    "Property would not be serialized into a 'Parcel'. Add '@Transient' annotation to it")
        }
    }

    override fun getMap() = MAP
}