// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation

enum class TestSubset {
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

fun contractTagOf(subset: TestSubset): String? = when (subset) {
    TestSubset.AllTests -> null
    TestSubset.SmokeTests -> null
    TestSubset.ContractTestsForCompilerInfrastructure -> "contract:CompilerInfrastructure"
    TestSubset.ContractTestsForFrontend -> "contract:Frontend"
    TestSubset.ContractTestsForCommonBackend -> "contract:CommonBackend"
    TestSubset.ContractTestsForJvm -> "contract:Jvm"
    TestSubset.ContractTestsForWasm -> "contract:Wasm"
    TestSubset.ContractTestsForJs -> "contract:Js"
    TestSubset.ContractTestsForNative -> "contract:Native"
    TestSubset.ContractTestsForCoreLibs -> "contract:CoreLibs"
    TestSubset.ContractTestsForAnalysisApi -> "contract:AnalysisApi"
    TestSubset.ContractTestsForBuildToolsApi -> "contract:BuildToolsApi"
    TestSubset.ContractTestsForSwiftExport -> "contract:SwiftExport"
    TestSubset.ContractTestsForCompilerPlugins -> "contract:CompilerPlugins"
    TestSubset.ContractTestsForGradle -> "contract:Gradle"
    TestSubset.ContractTestsForMaven -> "contract:Maven"
    TestSubset.ContractTestsForIntelliJ -> "contract:IntelliJ"
    TestSubset.ContractTestsForBuildInfrastructure -> "contract:BuildInfrastructure"
    TestSubset.ContractTestsForUnknown -> "contract:Unknown"
}
