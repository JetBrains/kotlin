// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation
import org.junit.jupiter.api.Tag

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.CompilerInfrastructure] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:CompilerInfrastructure")
annotation class MustRunOnChangesInCompilerInfrastructure

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Frontend] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Frontend")
annotation class MustRunOnChangesInFrontend

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.CommonBackend] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:CommonBackend")
annotation class MustRunOnChangesInCommonBackend

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Jvm] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Jvm")
annotation class MustRunOnChangesInJvm

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Wasm] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Wasm")
annotation class MustRunOnChangesInWasm

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Js] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Js")
annotation class MustRunOnChangesInJs

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Native] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Native")
annotation class MustRunOnChangesInNative

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.CoreLibs] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:CoreLibs")
annotation class MustRunOnChangesInCoreLibs

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.AnalysisApi] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:AnalysisApi")
annotation class MustRunOnChangesInAnalysisApi

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.BuildToolsApi] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:BuildToolsApi")
annotation class MustRunOnChangesInBuildToolsApi

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.SwiftExport] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:SwiftExport")
annotation class MustRunOnChangesInSwiftExport

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.CompilerPlugins] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:CompilerPlugins")
annotation class MustRunOnChangesInCompilerPlugins

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Gradle] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Gradle")
annotation class MustRunOnChangesInGradle

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Maven] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:Maven")
annotation class MustRunOnChangesInMaven

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.IntelliJ] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:IntelliJ")
annotation class MustRunOnChangesInIntelliJ

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.BuildInfrastructure] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
*/
@Tag("contract:BuildInfrastructure")
annotation class MustRunOnChangesInBuildInfrastructure

/**
* Requires the annotated tests to run and pass before merging to master when [Domain.Unknown] contains changed files.
* The tests still run whenever all tests in their own domain must run.
* Other test filters, including [NightlyTest], still apply.
*
* ### Extra: Contract tests
* Use this annotation for tests that check behavior another domain relies on.
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