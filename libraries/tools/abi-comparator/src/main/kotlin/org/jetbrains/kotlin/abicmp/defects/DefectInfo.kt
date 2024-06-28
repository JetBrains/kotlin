/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.defects

class DefectAttribute(val id: String, val htmlId: String)

class DefectType(
    val id: String,
    val messageText: String,
    vararg val requiredAttributes: DefectAttribute,
) : Comparable<DefectType> {
    override fun compareTo(other: DefectType): Int =
        id.compareTo(other.id)

    override fun hashCode(): Int = id.hashCode()
}

class DefectInfo(
    val type: DefectType,
    val attributes: Map<DefectAttribute, String>,
) : Comparable<DefectInfo> {
    init {
        for (requiredAttribute in type.requiredAttributes) {
            if (requiredAttribute !in attributes) {
                throw IllegalArgumentException(
                    "Missing required attribute ${requiredAttribute.id} for defect type ${type.id}"
                )
            }
        }
    }

    operator fun get(key: DefectAttribute) = attributes[key]

    override fun compareTo(other: DefectInfo): Int =
        when (val typeCmp = type.compareTo(other.type)) {
            0 -> compareAttributes(other)
            else -> typeCmp
        }

    private fun compareAttributes(other: DefectInfo): Int {
        for ((key, value) in attributes) {
            val otherValue = other[key] ?: return 1
            when (val valueCmp = value.compareTo(otherValue)) {
                0 -> continue
                else -> return valueCmp
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean =
        other is DefectInfo && compareTo(other) == 0

    override fun hashCode(): Int =
        type.hashCode()
}
