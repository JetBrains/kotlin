/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.incremental.web.IncrementalDataProviderImpl
import org.jetbrains.kotlin.incremental.web.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.hasFilesToRecompile
import org.jetbrains.kotlin.utils.JsMetadataVersion

class JsIncrementalEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.hasFilesToRecompile()) {
            val incrementalData = testServices.jsClassicIncrementalDataProvider.getIncrementalDataIfAny(module)
            val header = incrementalData?.header
            if (header != null) {
                configuration.put(
                    WebConfigurationKeys.INCREMENTAL_DATA_PROVIDER,
                    IncrementalDataProviderImpl(
                        header,
                        incrementalData.translatedFiles,
                        JsMetadataVersion.INSTANCE.toArray(),
                        incrementalData.packageMetadata,
                        emptyMap()
                    )
                )
            }

            configuration.put(WebConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, IncrementalResultsConsumerImpl())
        }
    }
}
