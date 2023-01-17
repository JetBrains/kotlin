/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.ValueAnonymizer
import org.jetbrains.kotlin.statistics.anonymizeComponentVersion
import org.jetbrains.kotlin.statistics.sha256
import kotlin.math.abs


enum class StringOverridePolicy: IMetricContainerFactory<String> {
    OVERRIDE {
        override fun newMetricContainer(): IMetricContainer<String> = OverrideMetricContainer<String>()

        override fun fromStringRepresentation(state: String): IMetricContainer<String>? = OverrideMetricContainer(state)
    },
    OVERRIDE_VERSION_IF_NOT_SET {
        override fun newMetricContainer(): IMetricContainer<String> = OverrideVersionMetricContainer()

        override fun fromStringRepresentation(state: String): IMetricContainer<String>? = OverrideVersionMetricContainer(state)
    },
    CONCAT {
        override fun newMetricContainer(): IMetricContainer<String> = ConcatMetricContainer()

        override fun fromStringRepresentation(state: String): IMetricContainer<String>? = ConcatMetricContainer(state.split(ConcatMetricContainer.SEPARATOR))
    }

    //Should be useful counting container?
}

private fun applyIfLong(v: String, action: (Long) -> IMetricContainer<Long>) : IMetricContainer<Long>? {
    val longVal = v.toLongOrNull()
    return if (longVal == null) {
        null
    } else {
        action(longVal)
    }
}

enum class NumberOverridePolicy: IMetricContainerFactory<Long> {
    OVERRIDE {
        override fun newMetricContainer(): IMetricContainer<Long> = OverrideMetricContainer<Long>()

        override fun fromStringRepresentation(state: String): IMetricContainer<Long>? = applyIfLong(state) {
            OverrideMetricContainer(it)
        }
    },
    SUM {
        override fun newMetricContainer(): IMetricContainer<Long> = SumMetricContainer()

        override fun fromStringRepresentation(state: String): IMetricContainer<Long>? = applyIfLong(state) {
            SumMetricContainer(it)
        }
    },
    AVERAGE {
        override fun newMetricContainer(): IMetricContainer<Long> = AverageMetricContainer()

        override fun fromStringRepresentation(state: String): IMetricContainer<Long>? = applyIfLong(state) {
            AverageMetricContainer(it)
        }
    }
}

enum class BooleanOverridePolicy: IMetricContainerFactory<Boolean> {
    OVERRIDE {
        override fun newMetricContainer(): IMetricContainer<Boolean> = OverrideMetricContainer<Boolean>()

        override fun fromStringRepresentation(state: String): IMetricContainer<Boolean>? = OverrideMetricContainer(state.toBoolean())
    },
    OR {
        override fun newMetricContainer(): IMetricContainer<Boolean> = OrMetricContainer()

        override fun fromStringRepresentation(state: String): IMetricContainer<Boolean>? = OrMetricContainer(state.toBoolean())
    }

    // may be add disctribution counter metric container
}

enum class BooleanAnonymizationPolicy : ValueAnonymizer<Boolean> {
    SAFE {
        override fun anonymize(t: Boolean) = t
    }
}

abstract class StringAnonymizationPolicy : ValueAnonymizer<String> {

    abstract fun validationRegexp(): String

    class AllowedListAnonymizer(val allowedValues: Collection<String>) : StringAnonymizationPolicy() {
        companion object {
            const val UNEXPECTED_VALUE = "UNEXPECTED-VALUE"
        }

        override fun validationRegexp(): String {
            return "^((${UNEXPECTED_VALUE}|${allowedValues.joinToString("|")})${ConcatMetricContainer.SEPARATOR}?)+$"
        }

        override fun anonymize(t: String): String {
            return if (t.matches(Regex(validationRegexp()))) {
                t
            } else {
                t.split(ConcatMetricContainer.SEPARATOR).joinToString(ConcatMetricContainer.SEPARATOR) {
                    if (allowedValues.contains(it))
                        it
                    else
                        UNEXPECTED_VALUE
                }
            }
        }
    }

    class RegexControlled(private val regex: String, private val anonymizeInIde: Boolean) : StringAnonymizationPolicy() {
        override fun validationRegexp() = regex

        override fun anonymize(t: String) = t

        override fun anonymizeOnIdeSize() = anonymizeInIde

    }

    class ComponentVersionAnonymizer() : StringAnonymizationPolicy() {
        override fun validationRegexp() = "(\\d+).(\\d+).(\\d+)-?(dev|snapshot|m\\d?|rc\\d?|beta\\d?)?"

        override fun anonymize(t: String) = anonymizeComponentVersion(t)
    }
}

enum class NumberAnonymizationPolicy : ValueAnonymizer<Long> {
    SAFE {
        override fun anonymize(t: Long) = t
    },
    RANDOM_10_PERCENT {
        override fun anonymize(t: Long): Long {
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

