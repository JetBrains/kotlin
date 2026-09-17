// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'

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

val contractSubsets = setOf(
    TestSubset.ContractTestsForCompilerInfrastructure,
    TestSubset.ContractTestsForFrontend,
    TestSubset.ContractTestsForCommonBackend,
    TestSubset.ContractTestsForJvm,
    TestSubset.ContractTestsForWasm,
    TestSubset.ContractTestsForJs,
    TestSubset.ContractTestsForNative,
    TestSubset.ContractTestsForCoreLibs,
    TestSubset.ContractTestsForAnalysisApi,
    TestSubset.ContractTestsForBuildToolsApi,
    TestSubset.ContractTestsForSwiftExport,
    TestSubset.ContractTestsForCompilerPlugins,
    TestSubset.ContractTestsForGradle,
    TestSubset.ContractTestsForMaven,
    TestSubset.ContractTestsForIntelliJ,
    TestSubset.ContractTestsForBuildInfrastructure,
    TestSubset.ContractTestsForUnknown,
)