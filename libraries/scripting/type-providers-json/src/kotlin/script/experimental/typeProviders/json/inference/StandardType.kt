/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.json.inference

import kotlin.reflect.KClass

internal enum class StandardType {
    Any, String, Boolean, Int, Double
}

internal val StandardType.kClass: KClass<*>
    get() = when (this) {
        StandardType.Any -> Any::class
        StandardType.String -> String::class
        StandardType.Boolean -> Boolean::class
        StandardType.Int -> Int::class
        StandardType.Double -> Double::class
    }

internal fun StandardType.asResolved(): Inferred.JSONType = Inferred.JSONType.Standard(this)
