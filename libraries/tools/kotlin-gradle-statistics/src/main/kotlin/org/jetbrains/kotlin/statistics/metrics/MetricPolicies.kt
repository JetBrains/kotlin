/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.DEFAULT_SEPARATOR
import org.jetbrains.kotlin.statistics.ValueAnonymizer
import org.jetbrains.kotlin.statistics.anonymizeComponentVersion
import kotlin.math.abs

enum class StringListOverridePolicy {
    CONCAT,
}

enum class StringOverridePolicy {
    OVERRIDE,
    OVERRIDE_VERSION_IF_NOT_SET
    //Should be useful counting container?
}

private fun applyIfLong(v: String, action: (Long) -> IMetricContainer<Long>): IMetricContainer<Long>? {
    val longVal = v.toLongOrNull()
    return if (longVal == null) {
        null
    } else {
        action(longVal)
    }
}

enum class NumberOverridePolicy  {
    OVERRIDE,
    SUM,
    AVERAGE
}

enum class BooleanOverridePolicy {
    OVERRIDE,
    OR,

    // may be add disctribution counter metric container
}

enum class BooleanAnonymizationPolicy : ValueAnonymizer<Boolean> {
    SAFE {
        override fun anonymize(t: Boolean, separator: String) = t
    }
}

abstract class StringAnonymizationPolicy : ValueAnonymizer<String> {

    abstract fun validationRegexp(separator: String = DEFAULT_SEPARATOR): String

    class AllowedListAnonymizer(val allowedValues: Collection<String>) : StringAnonymizationPolicy() {
        companion object {
            const val UNEXPECTED_VALUE = "UNEXPECTED-VALUE"
        }

        override fun validationRegexp(separator: String): String {
            return "^((${UNEXPECTED_VALUE}|${allowedValues.joinToString("|")})($separator)?)+$"
        }

        override fun anonymize(t: String, separator: String): String {
            return if (t.matches(Regex(validationRegexp(separator)))) {
                t
            } else {
                t.split(separator).joinToString(separator.toString()) {
                    if (allowedValues.contains(it))
                        it
                    else
                        UNEXPECTED_VALUE
                }
            }
        }
    }

    class RegexControlled(private val regex: String, private val anonymizeInIde: Boolean) : StringAnonymizationPolicy() {
        override fun validationRegexp(separator: String): String = regex

        override fun anonymize(t: String, separator: String) = t

        override fun anonymizeOnIdeSize() = anonymizeInIde

    }

    class ComponentVersionAnonymizer() : StringAnonymizationPolicy() {
        override fun validationRegexp(separator: String): String = "(\\d+).(\\d+).(\\d+)-?(dev|snapshot|m\\d?|rc\\d?|beta\\d?)?"

        override fun anonymize(t: String, separator: String) = anonymizeComponentVersion(t)
    }
}

enum class NumberAnonymizationPolicy : ValueAnonymizer<Long> {
    SAFE {
        override fun anonymize(t: Long, separator: String) = t
    },
    RANDOM_10_PERCENT {
        override fun anonymize(t: Long, separator: String): Long {
            if (abs(t) < 10) return t
            val sign = if (t < 0)
                -1
            else
                1
            val absT = t * sign
            var div: Long = 1
            while (div * 10 < absT) {
                div *= 10
            }
            return sign * if (absT / div < 2)
                absT - absT % (div / 10)
            else
                absT - absT % div
        }
    }
}

