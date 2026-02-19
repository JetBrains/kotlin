/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import JsError
import kotlin.internal.UsedFromCompilerGeneratedCode

@JsName("Boolean")
@UsedFromCompilerGeneratedCode
internal external fun nativeBoolean(obj: Any?): Boolean

@UsedFromCompilerGeneratedCode
internal fun booleanInExternalLog(name: String, obj: dynamic) {
    if (jsTypeOf(obj) != "boolean") {
        console.asDynamic().error("Boolean expected for '$name', but actual:", obj)
    }
}

@UsedFromCompilerGeneratedCode
internal fun booleanInExternalException(name: String, obj: dynamic) {
    if (jsTypeOf(obj) != "boolean") {
        throw JsError("Boolean expected for '$name', but actual: $obj")
    }
}
