// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'

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

fun contractTestsClusterOf(domain: Domain): TestCluster = when (domain) {
    Domain.CompilerInfrastructure -> TestCluster.ContractTestsForCompilerInfrastructure
    Domain.Frontend -> TestCluster.ContractTestsForFrontend
    Domain.CommonBackend -> TestCluster.ContractTestsForCommonBackend
    Domain.Jvm -> TestCluster.ContractTestsForJvm
    Domain.Wasm -> TestCluster.ContractTestsForWasm
    Domain.Js -> TestCluster.ContractTestsForJs
    Domain.Native -> TestCluster.ContractTestsForNative
    Domain.CoreLibs -> TestCluster.ContractTestsForCoreLibs
    Domain.AnalysisApi -> TestCluster.ContractTestsForAnalysisApi
    Domain.BuildToolsApi -> TestCluster.ContractTestsForBuildToolsApi
    Domain.SwiftExport -> TestCluster.ContractTestsForSwiftExport
    Domain.CompilerPlugins -> TestCluster.ContractTestsForCompilerPlugins
    Domain.Gradle -> TestCluster.ContractTestsForGradle
    Domain.Maven -> TestCluster.ContractTestsForMaven
    Domain.IntelliJ -> TestCluster.ContractTestsForIntelliJ
    Domain.BuildInfrastructure -> TestCluster.ContractTestsForBuildInfrastructure
    Domain.Unknown -> TestCluster.ContractTestsForUnknown
}