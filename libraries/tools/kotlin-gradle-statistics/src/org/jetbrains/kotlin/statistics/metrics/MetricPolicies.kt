/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

import org.jetbrains.kotlin.statistics.ValueAnonymizer
import org.jetbrains.kotlin.statistics.anonymizeComponentVersion
import org.jetbrains.kotlin.statistics.sha256


enum class StringOverridePolicy: IMetricContainerFactory<String> {
    OVERRIDE {
        override fun newMetricContainer(): IMetricContainer<String> = OverrideMetricContainer<String>()

        override fun fromStringRepresentation(state: String): IMetricContainer<String>? = OverrideMetricContainer(state)
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

enum class StringAnonymizationPolicy : ValueAnonymizer<String> {
    SAFE {
        override fun anonymize(t: String) = t
    },
    SHA_256 {
        override fun anonymize(t: String) = sha256(t)
    },
    COMPONENT_VERSION {
        override fun anonymize(t: String) = anonymizeComponentVersion(t)
    }
}

enum class NumberAnonymizationPolicy : ValueAnonymizer<Long> {
    SAFE {
        override fun anonymize(t: Long) = t
    },
    RANDOM_10_PERCENT {
        override fun anonymize(t: Long) = (t + t * 0.1 * Math.random()).toLong()
    },
    RANDOM_01_PERCENT {
        override fun anonymize(t: Long) = (t + t * 0.01 * Math.random()).toLong()
    }
}

