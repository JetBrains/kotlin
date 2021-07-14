// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jetbrains.kotlin.cli.common.arguments.*
import org.junit.Test


class CompilerArgumentsContentProspectorTest {

    @Test
    fun testJVMArgumentsContent() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JVMCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JVMCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JVMCompilerArguments::class)

        assertContentEquals(flagProperties, k2JVMCompilerArgumentsFlagProperties) { sortedBy { it.name } }
        assertContentEquals(stringProperties, k2JVMCompilerArgumentsStringProperties) { sortedBy { it.name } }
        assertContentEquals(arrayProperties, k2JVMCompilerArgumentsArrayArgumentProperties) { sortedBy { it.name } }
    }

    @Test
    fun testMetadataArgumentsContent() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2MetadataCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2MetadataCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2MetadataCompilerArguments::class)

        assertContentEquals(flagProperties, k2MetadataCompilerArgumentsFlagProperties) { sortedBy { it.name } }
        assertContentEquals(stringProperties, k2MetadataCompilerArgumentsStringProperties) { sortedBy { it.name } }
        assertContentEquals(arrayProperties, k2MetadataCompilerArgumentsArrayProperties) { sortedBy { it.name } }
    }

    @Test
    fun testJsArgumentsContent() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JSCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JSCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JSCompilerArguments::class)

        assertContentEquals(flagProperties, k2JSCompilerArgumentsFlagProperties) { sortedBy { it.name } }
        assertContentEquals(stringProperties, k2JSCompilerArgumentsStringProperties) { sortedBy { it.name } }
        assert(arrayProperties.isEmpty()) { "Expected empty arrayProperties, but actual ${arrayProperties.joinToString { it.name }}" }
    }

    @Test
    fun testJsDceArgumentsContent() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JSDceArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JSDceArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JSDceArguments::class)

        assertContentEquals(flagProperties, k2JSDceCompilerArgumentsFlagProperties) { sortedBy { it.name } }
        assertContentEquals(stringProperties, k2JSDceCompilerArgumentsStringProperties) { sortedBy { it.name } }
        assertContentEquals(arrayProperties, k2JSDceCompilerArgumentsArrayProperties) { sortedBy { it.name } }
    }

    companion object {

        private val commonToolArgumentsFlagProperties = listOf(
            CommonToolArguments::allWarningsAsErrors,
            CommonToolArguments::extraHelp,
            CommonToolArguments::help,
            CommonToolArguments::suppressWarnings,
            CommonToolArguments::verbose,
            CommonToolArguments::version,
        )

        private val commonCompilerArgumentsFlagProperties = commonToolArgumentsFlagProperties + listOf(
            CommonCompilerArguments::allowKotlinPackage,
            CommonCompilerArguments::progressiveMode,
            CommonCompilerArguments::script,
            CommonCompilerArguments::noInline,
            CommonCompilerArguments::skipMetadataVersionCheck,
            CommonCompilerArguments::skipPrereleaseCheck,
            CommonCompilerArguments::allowKotlinPackage,
            CommonCompilerArguments::reportOutputFiles,
            CommonCompilerArguments::multiPlatform,
            CommonCompilerArguments::noCheckActual,
            CommonCompilerArguments::newInference,
            CommonCompilerArguments::inlineClasses,
            CommonCompilerArguments::polymorphicSignature,
            CommonCompilerArguments::legacySmartCastAfterTry,
            CommonCompilerArguments::effectSystem,
            CommonCompilerArguments::readDeserializedContracts,
            CommonCompilerArguments::properIeee754Comparisons,
            CommonCompilerArguments::reportPerf,
            CommonCompilerArguments::allowResultReturnType,
            CommonCompilerArguments::listPhases,
            CommonCompilerArguments::profilePhases,
            CommonCompilerArguments::checkPhaseConditions,
            CommonCompilerArguments::checkStickyPhaseConditions,
            CommonCompilerArguments::useFir,
            CommonCompilerArguments::useFirExtendedCheckers,
            CommonCompilerArguments::disableUltraLightClasses,
            CommonCompilerArguments::useMixedNamedArguments,
            CommonCompilerArguments::expectActualLinker,
            CommonCompilerArguments::disableDefaultScriptingPlugin,
            CommonCompilerArguments::inferenceCompatibility
        )

        private val commonCompilerArgumentsStringProperties = listOf(
            CommonCompilerArguments::intellijPluginRoot,
            CommonCompilerArguments::dumpPerf,
            CommonCompilerArguments::metadataVersion,
            CommonCompilerArguments::dumpDirectory,
            CommonCompilerArguments::dumpOnlyFqName,
            CommonCompilerArguments::explicitApi
        )
        private val commonCompilerArgumentsArrayArgumentProperties = listOf(
            CommonCompilerArguments::pluginOptions,
            CommonCompilerArguments::pluginClasspaths,
            CommonCompilerArguments::experimental,
            CommonCompilerArguments::useExperimental,
            CommonCompilerArguments::optIn,
            CommonCompilerArguments::commonSources,
            CommonCompilerArguments::disablePhases,
            CommonCompilerArguments::verbosePhases,
            CommonCompilerArguments::phasesToDumpBefore,
            CommonCompilerArguments::phasesToDumpAfter,
            CommonCompilerArguments::phasesToDump,
            CommonCompilerArguments::namesExcludedFromDumping,
            CommonCompilerArguments::phasesToValidateBefore,
            CommonCompilerArguments::phasesToValidateAfter,
            CommonCompilerArguments::phasesToValidate,
        )

        private val k2JVMCompilerArgumentsFlagProperties = commonCompilerArgumentsFlagProperties + listOf(
            K2JVMCompilerArguments::includeRuntime,
            K2JVMCompilerArguments::noJdk,
            K2JVMCompilerArguments::noStdlib,
            K2JVMCompilerArguments::noReflect,
            K2JVMCompilerArguments::javaParameters,
            K2JVMCompilerArguments::useIR,
            K2JVMCompilerArguments::useOldBackend,
            K2JVMCompilerArguments::allowUnstableDependencies,
            K2JVMCompilerArguments::doNotClearBindingContext,
            K2JVMCompilerArguments::noCallAssertions,
            K2JVMCompilerArguments::noReceiverAssertions,
            K2JVMCompilerArguments::noParamAssertions,
            K2JVMCompilerArguments::strictJavaNullabilityAssertions,
            K2JVMCompilerArguments::noOptimize,
            K2JVMCompilerArguments::inheritMultifileParts,
            K2JVMCompilerArguments::useTypeTable,
            K2JVMCompilerArguments::skipRuntimeVersionCheck,
            K2JVMCompilerArguments::useOldClassFilesReading,
            K2JVMCompilerArguments::singleModule,
            K2JVMCompilerArguments::suppressMissingBuiltinsError,
            K2JVMCompilerArguments::useJavac,
            K2JVMCompilerArguments::compileJava,
            K2JVMCompilerArguments::noExceptionOnExplicitEqualsForBoxedNull,
            K2JVMCompilerArguments::disableStandardScript,
            K2JVMCompilerArguments::strictMetadataVersionSemantics,
            K2JVMCompilerArguments::sanitizeParentheses,
            K2JVMCompilerArguments::allowNoSourceFiles,
            K2JVMCompilerArguments::emitJvmTypeAnnotations,
            K2JVMCompilerArguments::noOptimizedCallableReferences,
            K2JVMCompilerArguments::noKotlinNothingValueException,
            K2JVMCompilerArguments::noResetJarTimestamps,
            K2JVMCompilerArguments::noUnifiedNullChecks,
            K2JVMCompilerArguments::useOldSpilledVarTypeAnalysis,
            K2JVMCompilerArguments::useOldInlineClassesManglingScheme,
            K2JVMCompilerArguments::enableJvmPreview,
        )

        private val k2JVMCompilerArgumentsStringProperties = commonCompilerArgumentsStringProperties + listOf(
            K2JVMCompilerArguments::destination,
            K2JVMCompilerArguments::classpath,
            K2JVMCompilerArguments::jdkHome,
            K2JVMCompilerArguments::expression,
            K2JVMCompilerArguments::moduleName,
            K2JVMCompilerArguments::jvmTarget,
            K2JVMCompilerArguments::abiStability,
            K2JVMCompilerArguments::javaModulePath,
            K2JVMCompilerArguments::constructorCallNormalizationMode,
            K2JVMCompilerArguments::assertionsMode,
            K2JVMCompilerArguments::buildFile,
            K2JVMCompilerArguments::declarationsOutputPath,
            K2JVMCompilerArguments::javaPackagePrefix,
            K2JVMCompilerArguments::supportCompatqualCheckerFrameworkAnnotations,
            K2JVMCompilerArguments::jspecifyAnnotations,
            K2JVMCompilerArguments::jvmDefault,
            K2JVMCompilerArguments::defaultScriptExtension,
            K2JVMCompilerArguments::stringConcat,
            K2JVMCompilerArguments::klibLibraries,
            K2JVMCompilerArguments::profileCompilerCommand,
            K2JVMCompilerArguments::repeatCompileModules
        )

        private val k2JVMCompilerArgumentsArrayArgumentProperties = commonCompilerArgumentsArrayArgumentProperties + listOf(
            K2JVMCompilerArguments::scriptTemplates,
            K2JVMCompilerArguments::additionalJavaModules,
            K2JVMCompilerArguments::scriptResolverEnvironment,
            K2JVMCompilerArguments::javacArguments,
            K2JVMCompilerArguments::javaSourceRoots,
            K2JVMCompilerArguments::jsr305,
            K2JVMCompilerArguments::friendPaths
        )

        private val k2MetadataCompilerArgumentsFlagProperties = commonCompilerArgumentsFlagProperties + listOf(
            K2MetadataCompilerArguments::enabledInJps
        )
        private val k2MetadataCompilerArgumentsStringProperties = commonCompilerArgumentsStringProperties + listOf(
            K2MetadataCompilerArguments::destination,
            K2MetadataCompilerArguments::classpath,
            K2MetadataCompilerArguments::moduleName
        )
        private val k2MetadataCompilerArgumentsArrayProperties = commonCompilerArgumentsArrayArgumentProperties + listOf(
            K2MetadataCompilerArguments::friendPaths,
            K2MetadataCompilerArguments::refinesPaths
        )
        private val k2JSCompilerArgumentsFlagProperties = commonCompilerArgumentsFlagProperties + listOf(
            K2JSCompilerArguments::noStdlib,
            K2JSCompilerArguments::sourceMap,
            K2JSCompilerArguments::metaInfo,
            K2JSCompilerArguments::irProduceKlibDir,
            K2JSCompilerArguments::irProduceKlibFile,
            K2JSCompilerArguments::irProduceJs,
            K2JSCompilerArguments::irDce,
            K2JSCompilerArguments::irDceDriven,
            K2JSCompilerArguments::irDcePrintReachabilityInfo,
            K2JSCompilerArguments::irPropertyLazyInitialization,
            K2JSCompilerArguments::irOnly,
            K2JSCompilerArguments::irPerModule,
            K2JSCompilerArguments::generateDts,
            K2JSCompilerArguments::typedArrays,
            K2JSCompilerArguments::friendModulesDisabled,
            K2JSCompilerArguments::metadataOnly,
            K2JSCompilerArguments::enableJsScripting,
            K2JSCompilerArguments::fakeOverrideValidator,
            K2JSCompilerArguments::wasm
        )
        private val k2JSCompilerArgumentsStringProperties = commonCompilerArgumentsStringProperties + listOf(
            K2JSCompilerArguments::outputFile,
            K2JSCompilerArguments::libraries,
            K2JSCompilerArguments::sourceMapPrefix,
            K2JSCompilerArguments::sourceMapBaseDirs,
            K2JSCompilerArguments::sourceMapEmbedSources,
            K2JSCompilerArguments::target,
            K2JSCompilerArguments::moduleKind,
            K2JSCompilerArguments::main,
            K2JSCompilerArguments::outputPrefix,
            K2JSCompilerArguments::outputPostfix,
            K2JSCompilerArguments::irModuleName,
            K2JSCompilerArguments::includes,
            K2JSCompilerArguments::friendModules,
            K2JSCompilerArguments::errorTolerancePolicy,
        )

        private val k2JSDceCompilerArgumentsFlagProperties = commonToolArgumentsFlagProperties + listOf(
            K2JSDceArguments::devMode,
            K2JSDceArguments::printReachabilityInfo,
        )
        private val k2JSDceCompilerArgumentsStringProperties = listOf(
            K2JSDceArguments::outputDirectory,
            K2JSDceArguments::devModeOverwritingStrategy,
        )
        private val k2JSDceCompilerArgumentsArrayProperties = listOf(
            K2JSDceArguments::declarationsToKeep
        )

        private fun <T> assertContentEquals(expect: Iterable<T>, actual: Iterable<T>, preprocessor: Iterable<T>.() -> Iterable<T>) {
            (expect.count() == actual.count()) && preprocessor(expect).zip(preprocessor(actual)).all { it.first == it.second }
        }

    }
}