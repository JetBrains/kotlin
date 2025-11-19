package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.loader.KlibManifestTransformer
import java.util.*

class KlibNativeManifestTransformer(private val target: KonanTarget) : KlibManifestTransformer {
    override fun transform(manifestProperties: Properties) = manifestProperties.substituteFor(target)
}

fun Properties.substituteFor(target: KonanTarget): Properties {
    val tagetSuffix = target.visibleName
    val archSuffix = target.architecture.visibleName
    val familySuffix = target.family.visibleName

    val substitutionMap: MutableMap</* base property name */ String, Substitutions> = hashMapOf()

    val result = Properties()

    for (propertyName in stringPropertyNames()) {
        val propertyValue = getProperty(propertyName)
        result[propertyName] = propertyValue

        val dotIndex = propertyName.indexOf('.')
        if (dotIndex != -1) {
            val basePropertyName = propertyName.take(dotIndex)
            if (basePropertyName in DefFileProperty.substitutablePropertyNames) {
                fun getSubstitution() = substitutionMap.getOrPut(basePropertyName) { Substitutions() }

                when (propertyName.substring(dotIndex + 1)) {
                    tagetSuffix -> getSubstitution().targetValue = propertyValue
                    archSuffix -> getSubstitution().archValue = propertyValue
                    familySuffix -> getSubstitution().familyValue = propertyValue
                }
            }
        }
    }

    for ((basePropertyName, substitutions) in substitutionMap) {
        result[basePropertyName] = listOfNotNull(
            result.getProperty(basePropertyName),
            substitutions.targetValue,
            substitutions.archValue,
            substitutions.familyValue
        ).joinToString(" ")
    }

    return result
}

private class Substitutions {
    var targetValue: String? = null
    var archValue: String? = null
    var familyValue: String? = null
}
