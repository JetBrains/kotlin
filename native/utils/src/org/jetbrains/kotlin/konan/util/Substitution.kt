package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

fun Properties.substituteFor(target: KonanTarget): Properties {
    val result = Properties()
    result.putAll(this)
    substitute(result, defaultTargetSubstitutions(target))
    return result
}

private fun defaultTargetSubstitutions(target: KonanTarget) =
        mapOf<String, String>(
            "target" to target.visibleName,
            "arch" to target.architecture.visibleName,
            "family" to target.family.visibleName)

// Performs substitution similar to:
//  foo = ${foo} ${foo.${arch}} ${foo.${os}}
private fun substitute(properties: Properties, substitutions: Map<String, String>) {
    val propertyNames = properties.stringPropertyNames().toList()
    for (substitution in substitutions.values) {
        for (key in propertyNames) {
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
