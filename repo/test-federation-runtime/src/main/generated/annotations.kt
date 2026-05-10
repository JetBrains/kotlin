// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation
import org.junit.jupiter.api.Tag

/**
* Will mark tests as 'affected by' the given domain [Domain.Compiler].
* Such tests will run, additionally, for all commits affecting the Compiler domain.
*/
@Tag("affectedBy:Compiler")
annotation class AffectedByCompiler

/**
* Will mark tests as 'affected by' the given domain [Domain.Wasm].
* Such tests will run, additionally, for all commits affecting the Wasm domain.
*/
@Tag("affectedBy:Wasm")
annotation class AffectedByWasm

/**
* Will mark tests as 'affected by' the given domain [Domain.Js].
* Such tests will run, additionally, for all commits affecting the Js domain.
*/
@Tag("affectedBy:Js")
annotation class AffectedByJs

/**
* Will mark tests as 'affected by' the given domain [Domain.Native].
* Such tests will run, additionally, for all commits affecting the Native domain.
*/
@Tag("affectedBy:Native")
annotation class AffectedByNative

/**
* Will mark tests as 'affected by' the given domain [Domain.AnalysisApi].
* Such tests will run, additionally, for all commits affecting the AnalysisApi domain.
*/
@Tag("affectedBy:AnalysisApi")
annotation class AffectedByAnalysisApi

/**
* Will mark tests as 'affected by' the given domain [Domain.SwiftExport].
* Such tests will run, additionally, for all commits affecting the SwiftExport domain.
*/
@Tag("affectedBy:SwiftExport")
annotation class AffectedBySwiftExport

/**
* Will mark tests as 'affected by' the given domain [Domain.CompilerPlugins].
* Such tests will run, additionally, for all commits affecting the CompilerPlugins domain.
*/
@Tag("affectedBy:CompilerPlugins")
annotation class AffectedByCompilerPlugins

/**
* Will mark tests as 'affected by' the given domain [Domain.Gradle].
* Such tests will run, additionally, for all commits affecting the Gradle domain.
*/
@Tag("affectedBy:Gradle")
annotation class AffectedByGradle

/**
* Will mark tests as 'affected by' the given domain [Domain.Maven].
* Such tests will run, additionally, for all commits affecting the Maven domain.
*/
@Tag("affectedBy:Maven")
annotation class AffectedByMaven

/**
* Will mark tests as 'affected by' the given domain [Domain.IntelliJ].
* Such tests will run, additionally, for all commits affecting the IntelliJ domain.
*/
@Tag("affectedBy:IntelliJ")
annotation class AffectedByIntelliJ

/**
* Will mark tests as 'affected by' the given domain [Domain.BuildInfrastructure].
* Such tests will run, additionally, for all commits affecting the BuildInfrastructure domain.
*/
@Tag("affectedBy:BuildInfrastructure")
annotation class AffectedByBuildInfrastructure

/**
* Will mark tests as 'affected by' the given domain [Domain.Unknown].
* Such tests will run, additionally, for all commits affecting the Unknown domain.
*/
@Tag("affectedBy:Unknown")
annotation class AffectedByUnknown

fun affectedByAnnotationOf(domain: Domain) = when (domain) {
    Domain.Compiler -> AffectedByCompiler::class
    Domain.Wasm -> AffectedByWasm::class
    Domain.Js -> AffectedByJs::class
    Domain.Native -> AffectedByNative::class
    Domain.AnalysisApi -> AffectedByAnalysisApi::class
    Domain.SwiftExport -> AffectedBySwiftExport::class
    Domain.CompilerPlugins -> AffectedByCompilerPlugins::class
    Domain.Gradle -> AffectedByGradle::class
    Domain.Maven -> AffectedByMaven::class
    Domain.IntelliJ -> AffectedByIntelliJ::class
    Domain.BuildInfrastructure -> AffectedByBuildInfrastructure::class
    Domain.Unknown -> AffectedByUnknown::class
}