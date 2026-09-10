// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation
import org.junit.jupiter.api.Tag

/**
* Tests must additionally run when the given domain [Domain.CompilerInfrastructure] contains changed files.
*/
@Tag("contract:CompilerInfrastructure")
annotation class MustRunOnChangesInCompilerInfrastructure

/**
* Tests must additionally run when the given domain [Domain.Frontend] contains changed files.
*/
@Tag("contract:Frontend")
annotation class MustRunOnChangesInFrontend

/**
* Tests must additionally run when the given domain [Domain.CommonBackend] contains changed files.
*/
@Tag("contract:CommonBackend")
annotation class MustRunOnChangesInCommonBackend

/**
* Tests must additionally run when the given domain [Domain.Jvm] contains changed files.
*/
@Tag("contract:Jvm")
annotation class MustRunOnChangesInJvm

/**
* Tests must additionally run when the given domain [Domain.Wasm] contains changed files.
*/
@Tag("contract:Wasm")
annotation class MustRunOnChangesInWasm

/**
* Tests must additionally run when the given domain [Domain.Js] contains changed files.
*/
@Tag("contract:Js")
annotation class MustRunOnChangesInJs

/**
* Tests must additionally run when the given domain [Domain.Native] contains changed files.
*/
@Tag("contract:Native")
annotation class MustRunOnChangesInNative

/**
* Tests must additionally run when the given domain [Domain.CoreLibs] contains changed files.
*/
@Tag("contract:CoreLibs")
annotation class MustRunOnChangesInCoreLibs

/**
* Tests must additionally run when the given domain [Domain.AnalysisApi] contains changed files.
*/
@Tag("contract:AnalysisApi")
annotation class MustRunOnChangesInAnalysisApi

/**
* Tests must additionally run when the given domain [Domain.BuildToolsApi] contains changed files.
*/
@Tag("contract:BuildToolsApi")
annotation class MustRunOnChangesInBuildToolsApi

/**
* Tests must additionally run when the given domain [Domain.SwiftExport] contains changed files.
*/
@Tag("contract:SwiftExport")
annotation class MustRunOnChangesInSwiftExport

/**
* Tests must additionally run when the given domain [Domain.CompilerPlugins] contains changed files.
*/
@Tag("contract:CompilerPlugins")
annotation class MustRunOnChangesInCompilerPlugins

/**
* Tests must additionally run when the given domain [Domain.Gradle] contains changed files.
*/
@Tag("contract:Gradle")
annotation class MustRunOnChangesInGradle

/**
* Tests must additionally run when the given domain [Domain.Maven] contains changed files.
*/
@Tag("contract:Maven")
annotation class MustRunOnChangesInMaven

/**
* Tests must additionally run when the given domain [Domain.IntelliJ] contains changed files.
*/
@Tag("contract:IntelliJ")
annotation class MustRunOnChangesInIntelliJ

/**
* Tests must additionally run when the given domain [Domain.BuildInfrastructure] contains changed files.
*/
@Tag("contract:BuildInfrastructure")
annotation class MustRunOnChangesInBuildInfrastructure

/**
* Tests must additionally run when the given domain [Domain.Unknown] contains changed files.
*/
@Tag("contract:Unknown")
annotation class MustRunOnChangesInUnknown

fun mustRunOnChangesInAnnotationOf(domain: Domain) = when (domain) {
    Domain.CompilerInfrastructure -> MustRunOnChangesInCompilerInfrastructure::class
    Domain.Frontend -> MustRunOnChangesInFrontend::class
    Domain.CommonBackend -> MustRunOnChangesInCommonBackend::class
    Domain.Jvm -> MustRunOnChangesInJvm::class
    Domain.Wasm -> MustRunOnChangesInWasm::class
    Domain.Js -> MustRunOnChangesInJs::class
    Domain.Native -> MustRunOnChangesInNative::class
    Domain.CoreLibs -> MustRunOnChangesInCoreLibs::class
    Domain.AnalysisApi -> MustRunOnChangesInAnalysisApi::class
    Domain.BuildToolsApi -> MustRunOnChangesInBuildToolsApi::class
    Domain.SwiftExport -> MustRunOnChangesInSwiftExport::class
    Domain.CompilerPlugins -> MustRunOnChangesInCompilerPlugins::class
    Domain.Gradle -> MustRunOnChangesInGradle::class
    Domain.Maven -> MustRunOnChangesInMaven::class
    Domain.IntelliJ -> MustRunOnChangesInIntelliJ::class
    Domain.BuildInfrastructure -> MustRunOnChangesInBuildInfrastructure::class
    Domain.Unknown -> MustRunOnChangesInUnknown::class
}