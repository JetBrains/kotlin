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

package org.jetbrains.kotlin.js

import kotlin.properties.Delegates
import org.jetbrains.kotlin.name.FqName

public enum class PredefinedAnnotation(fqName: String) {
    LIBRARY : PredefinedAnnotation("kotlin.js.library")
    NATIVE : PredefinedAnnotation("kotlin.js.native")
    NATIVE_INVOKE : PredefinedAnnotation("kotlin.js.nativeInvoke")
    NATIVE_GETTER : PredefinedAnnotation("kotlin.js.nativeGetter")
    NATIVE_SETTER : PredefinedAnnotation("kotlin.js.nativeSetter")

    public val fqName: FqName = FqName(fqName)

    default object {
        // TODO: replace with straight assignment when KT-5761 will be fixed
        val WITH_CUSTOM_NAME by Delegates.lazy { setOf(LIBRARY, NATIVE) }
    }
}
