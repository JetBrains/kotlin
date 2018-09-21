/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.attributes.*

open class Compatible: AttributeCompatibilityRule<Boolean> {
    override fun execute(details: CompatibilityCheckDetails<Boolean>) = details.compatible()
}

open class PreferValue(val defaultPreferred: Boolean): AttributeDisambiguationRule<Boolean> {
    override fun execute(details: MultipleCandidatesDetails<Boolean>) = with(details) {
        val preferredValue = consumerValue ?: defaultPreferred

        if (candidateValues.contains(preferredValue)) {
            closestMatch(preferredValue)
        } else {
            closestMatch(!preferredValue)
        }
    }
}

open class DebuggableDisambiguation: PreferValue(false)
open class OptimizedDisambiguation: PreferValue(false)

open class UsageCompatibility: AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) = with(details) {
        val requested = consumerValue?.name
        val provided = producerValue?.name

        when {
            requested == null -> compatible()
            requested == Usage.JAVA_API && (provided == Usage.JAVA_API || provided == KotlinNativeUsage.KLIB) -> {
                compatible()
            }
            requested == provided -> compatible()
        }
    }
}

open class UsageDisambiguation: AttributeDisambiguationRule<Usage> {
    override fun execute(details: MultipleCandidatesDetails<Usage>): Unit = with(details) {
        val usagePriority = listOf(Usage.JAVA_API, KotlinNativeUsage.KLIB)
        usagePriority.forEach { usage ->
            val found = candidateValues.find { it.name == usage }
            if (found != null) {
                closestMatch(found)
                return
            }
        }
    }
}

