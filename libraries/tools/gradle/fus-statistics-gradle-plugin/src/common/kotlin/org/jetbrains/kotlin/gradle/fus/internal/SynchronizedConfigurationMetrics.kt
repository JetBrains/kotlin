/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

class SynchronizedConfigurationMetrics<T : Any>(private val configurationMetrics: ListProperty<T>, val logger: Logger) {
    private var configurationMetricsStored = false

    companion object {
        private const val CONFIGURATION_METRICS_HAVE_ALREADY_STORED_MESSAGE =
            "Configuration metrics have already been stored in the file. Skipping further updates."
    }

    fun getConfigurationMetrics(): List<T> {
        synchronized(this) {
            configurationMetricsStored = true
            return configurationMetrics.get()
        }
    }

    fun addAll(metrics: List<T>) {
        collectMetricsIfNotStored {
            configurationMetrics.addAll(metrics)
        }
    }

    fun addAll(metrics: Provider<List<T>>) {
        collectMetricsIfNotStored {
            configurationMetrics.addAll(metrics)
        }
    }

    fun add(metric: T) {
        collectMetricsIfNotStored {
            configurationMetrics.add(metric)
        }
    }

    fun add(metric: Provider<T>) {
        collectMetricsIfNotStored {
            configurationMetrics.add(metric)
        }
    }

    private fun collectMetricsIfNotStored(action: () -> Unit) {
        if (configurationMetricsStored) {
            logger.debug(CONFIGURATION_METRICS_HAVE_ALREADY_STORED_MESSAGE)
            return
        }
        synchronized(this) {
            if (configurationMetricsStored) {
                logger.debug(CONFIGURATION_METRICS_HAVE_ALREADY_STORED_MESSAGE)
                return
            }
            action()
        }
    }
}
