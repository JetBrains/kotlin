package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.initIdeaConfiguration
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.junit.jupiter.api.BeforeAll

abstract class BaseTestRunner : AbstractKotlinCompilerTest() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            initIdeaConfiguration()
        }
    }

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }
}

//fun TestConfigurationBuilder.commonFirWithPluginFrontendConfiguration() {
//    baseFirDiagnosticTestConfiguration()
//
//    defaultDirectives {
//        +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
//        +FirDiagnosticsDirectives.FIR_DUMP
//    }
//
//    useConfigurators(
//        ::PluginAnnotationsProvider,
//        ::ExtensionRegistrarConfigurator
//    )
//}
