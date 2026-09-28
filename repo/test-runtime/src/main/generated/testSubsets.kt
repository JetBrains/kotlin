// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation

enum class TestSubset {
    AllTests,
    PlainTests,
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

fun contractTestsSubsetOf(domain: Domain): TestSubset = when (domain) {
    Domain.CompilerInfrastructure -> TestSubset.ContractTestsForCompilerInfrastructure
    Domain.Frontend -> TestSubset.ContractTestsForFrontend
    Domain.CommonBackend -> TestSubset.ContractTestsForCommonBackend
    Domain.Jvm -> TestSubset.ContractTestsForJvm
    Domain.Wasm -> TestSubset.ContractTestsForWasm
    Domain.Js -> TestSubset.ContractTestsForJs
    Domain.Native -> TestSubset.ContractTestsForNative
    Domain.CoreLibs -> TestSubset.ContractTestsForCoreLibs
    Domain.AnalysisApi -> TestSubset.ContractTestsForAnalysisApi
    Domain.BuildToolsApi -> TestSubset.ContractTestsForBuildToolsApi
    Domain.SwiftExport -> TestSubset.ContractTestsForSwiftExport
    Domain.CompilerPlugins -> TestSubset.ContractTestsForCompilerPlugins
    Domain.Gradle -> TestSubset.ContractTestsForGradle
    Domain.Maven -> TestSubset.ContractTestsForMaven
    Domain.IntelliJ -> TestSubset.ContractTestsForIntelliJ
    Domain.BuildInfrastructure -> TestSubset.ContractTestsForBuildInfrastructure
    Domain.Unknown -> TestSubset.ContractTestsForUnknown
}

fun contractTagOf(subset: TestSubset): String? = when (subset) {
    TestSubset.AllTests -> null
    TestSubset.PlainTests -> null
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

fun contractSubsetFromTag(tag: String): TestSubset? =
    when (tag) {
        "contract:CompilerInfrastructure" -> TestSubset.ContractTestsForCompilerInfrastructure
        "contract:Frontend" -> TestSubset.ContractTestsForFrontend
        "contract:CommonBackend" -> TestSubset.ContractTestsForCommonBackend
        "contract:Jvm" -> TestSubset.ContractTestsForJvm
        "contract:Wasm" -> TestSubset.ContractTestsForWasm
        "contract:Js" -> TestSubset.ContractTestsForJs
        "contract:Native" -> TestSubset.ContractTestsForNative
        "contract:CoreLibs" -> TestSubset.ContractTestsForCoreLibs
        "contract:AnalysisApi" -> TestSubset.ContractTestsForAnalysisApi
        "contract:BuildToolsApi" -> TestSubset.ContractTestsForBuildToolsApi
        "contract:SwiftExport" -> TestSubset.ContractTestsForSwiftExport
        "contract:CompilerPlugins" -> TestSubset.ContractTestsForCompilerPlugins
        "contract:Gradle" -> TestSubset.ContractTestsForGradle
        "contract:Maven" -> TestSubset.ContractTestsForMaven
        "contract:IntelliJ" -> TestSubset.ContractTestsForIntelliJ
        "contract:BuildInfrastructure" -> TestSubset.ContractTestsForBuildInfrastructure
        "contract:Unknown" -> TestSubset.ContractTestsForUnknown
        else -> null
    }