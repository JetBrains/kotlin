/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

fun defaultTargetSubstitutions(target: KonanTarget) =
    mapOf(
        "target" to target.visibleName,
        "arch" to target.architecture.visibleName,
        "family" to target.family.visibleName
    )

// Performs substitution similar to:
//  foo = ${foo} ${foo.${arch}} ${foo.${os}}
fun substitute(properties: Properties, substitutions: Map<String, String>) {
    for (key in properties.stringPropertyNames()) {
        for (substitution in substitutions.values) {
            val suffix = ".$substitution"
            if (key.endsWith(suffix)) {
                val baseKey = key.removeSuffix(suffix)
                val oldValue = properties.getProperty(baseKey, "")
                val appendedValue = properties.getProperty(key, "")
                val newValue = if (oldValue != "") "$oldValue $appendedValue" else appendedValue
                properties.setProperty(baseKey, newValue)
            }
        }
    }
}
