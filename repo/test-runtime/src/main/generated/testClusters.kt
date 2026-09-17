// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation

enum class TestCluster {
    AllTests,
    SmokeTests,
    ContractTestsForCompilerInfrastructure,
    ContractTestsForFrontend,
    ContractTestsForCommonBackend,
    ContractTestsForJvm,
    ContractTestsForWasm,
    ContractTestsForJs,
    ContractTestsForNative,
    ContractTestsForCoreLibs,
    ContractTestsForAnalysisApi,
    ContractTestsForBuildToolsApi,
    ContractTestsForSwiftExport,
    ContractTestsForCompilerPlugins,
    ContractTestsForGradle,
    ContractTestsForMaven,
    ContractTestsForIntelliJ,
    ContractTestsForBuildInfrastructure,
    ContractTestsForUnknown,
    ;
}

fun contractTagOf(cluster: TestCluster): String? = when (cluster) {
    TestCluster.AllTests -> null
    TestCluster.SmokeTests -> null
    TestCluster.ContractTestsForCompilerInfrastructure -> "contract:CompilerInfrastructure"
    TestCluster.ContractTestsForFrontend -> "contract:Frontend"
    TestCluster.ContractTestsForCommonBackend -> "contract:CommonBackend"
    TestCluster.ContractTestsForJvm -> "contract:Jvm"
    TestCluster.ContractTestsForWasm -> "contract:Wasm"
    TestCluster.ContractTestsForJs -> "contract:Js"
    TestCluster.ContractTestsForNative -> "contract:Native"
    TestCluster.ContractTestsForCoreLibs -> "contract:CoreLibs"
    TestCluster.ContractTestsForAnalysisApi -> "contract:AnalysisApi"
    TestCluster.ContractTestsForBuildToolsApi -> "contract:BuildToolsApi"
    TestCluster.ContractTestsForSwiftExport -> "contract:SwiftExport"
    TestCluster.ContractTestsForCompilerPlugins -> "contract:CompilerPlugins"
    TestCluster.ContractTestsForGradle -> "contract:Gradle"
    TestCluster.ContractTestsForMaven -> "contract:Maven"
    TestCluster.ContractTestsForIntelliJ -> "contract:IntelliJ"
    TestCluster.ContractTestsForBuildInfrastructure -> "contract:BuildInfrastructure"
    TestCluster.ContractTestsForUnknown -> "contract:Unknown"
}