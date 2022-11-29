// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jetbrains.kotlin.cli.common.arguments.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.reflect.KProperty


class CompilerArgumentsContentProspectorTest {

    @Test
    fun testJVMArgumentsContent() {
        if (true) {
            return // temporarily ignore the test KT-50594
        }
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JVMCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JVMCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JVMCompilerArguments::class)

        assertContentEquals(flagProperties, k2JVMCompilerArgumentsFlagProperties)
        assertContentEquals(stringProperties, k2JVMCompilerArgumentsStringProperties)
        assertContentEquals(arrayProperties, k2JVMCompilerArgumentsArrayArgumentProperties)
    }

    @Test
    fun testMetadataArgumentsContent() {
        if (true) {
            return // temporarily ignore the test KT-50594
        }
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2MetadataCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2MetadataCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2MetadataCompilerArguments::class)

        assertContentEquals(flagProperties, k2MetadataCompilerArgumentsFlagProperties)
        assertContentEquals(stringProperties, k2MetadataCompilerArgumentsStringProperties)
        assertContentEquals(arrayProperties, k2MetadataCompilerArgumentsArrayProperties)
    }

    @Test
    fun testJsArgumentsContent() {
        if (true) {
            return // temporarily ignore the test KT-50594
        }
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JSCompilerArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JSCompilerArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JSCompilerArguments::class)

        assertContentEquals(flagProperties, k2JSCompilerArgumentsFlagProperties)
        assertContentEquals(stringProperties, k2JSCompilerArgumentsStringProperties)
        assertContentEquals(arrayProperties, k2JSCompilerArgumentsArrayProperties)
    }

    @Test
    fun testJsDceArgumentsContent() {
        val flagProperties = CompilerArgumentsContentProspector.getFlagCompilerArgumentProperties(K2JSDceArguments::class)
        val stringProperties = CompilerArgumentsContentProspector.getStringCompilerArgumentProperties(K2JSDceArguments::class)
        val arrayProperties = CompilerArgumentsContentProspector.getArrayCompilerArgumentProperties(K2JSDceArguments::class)

        assertContentEquals(flagProperties, k2JSDceCompilerArgumentsFlagProperties)
        assertContentEquals(stringProperties, k2JSDceCompilerArgumentsStringProperties)
        assertContentEquals(arrayProperties, k2JSDceCompilerArgumentsArrayProperties)
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
            CommonCompilerArguments::reportOutputFiles,
            CommonCompilerArguments::multiPlatform,
            CommonCompilerArguments::noCheckActual,
            CommonCompilerArguments::newInference,
            CommonCompilerArguments::inlineClasses,
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
            CommonCompilerArguments::useK2,
            CommonCompilerArguments::useFirExtendedCheckers,
            CommonCompilerArguments::disableUltraLightClasses,
            CommonCompilerArguments::useMixedNamedArguments,
            CommonCompilerArguments::expectActualLinker,
            CommonCompilerArguments::extendedCompilerChecks,
            CommonCompilerArguments::disableDefaultScriptingPlugin,
            CommonCompilerArguments::inferenceCompatibility,
            CommonCompilerArguments::suppressVersionWarnings
        )

        private val commonCompilerArgumentsStringProperties = listOf(
            CommonCompilerArguments::languageVersion,
            CommonCompilerArguments::apiVersion,
            CommonCompilerArguments::intellijPluginRoot,
            CommonCompilerArguments::dumpPerf,
            CommonCompilerArguments::metadataVersion,
            CommonCompilerArguments::dumpDirectory,
            CommonCompilerArguments::dumpOnlyFqName,
            CommonCompilerArguments::explicitApi,
            CommonCompilerArguments::kotlinHome
        )
        private val commonCompilerArgumentsArrayProperties = listOf(
            CommonCompilerArguments::pluginOptions,
            CommonCompilerArguments::pluginClasspaths,
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
            K2JVMCompilerArguments::allowUnstableDependencies,
            K2JVMCompilerArguments::doNotClearBindingContext,
            K2JVMCompilerArguments::noCallAssertions,
            K2JVMCompilerArguments::noReceiverAssertions,
            K2JVMCompilerArguments::noParamAssertions,
            K2JVMCompilerArguments::noOptimize,
            K2JVMCompilerArguments::inheritMultifileParts,
            K2JVMCompilerArguments::useTypeTable,
            K2JVMCompilerArguments::useOldClassFilesReading,
            K2JVMCompilerArguments::suppressMissingBuiltinsError,
            K2JVMCompilerArguments::useJavac,
            K2JVMCompilerArguments::compileJava,
            K2JVMCompilerArguments::disableStandardScript,
            K2JVMCompilerArguments::strictMetadataVersionSemantics,
            K2JVMCompilerArguments::suppressDeprecatedJvmTargetWarning,
            K2JVMCompilerArguments::typeEnhancementImprovementsInStrictMode,
            K2JVMCompilerArguments::sanitizeParentheses,
            K2JVMCompilerArguments::allowNoSourceFiles,
            K2JVMCompilerArguments::emitJvmTypeAnnotations,
            K2JVMCompilerArguments::noOptimizedCallableReferences,
            K2JVMCompilerArguments::noKotlinNothingValueException,
            K2JVMCompilerArguments::noResetJarTimestamps,
            K2JVMCompilerArguments::noUnifiedNullChecks,
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
            K2JVMCompilerArguments::repeatCompileModules,
            K2JVMCompilerArguments::lambdas,
            K2JVMCompilerArguments::samConversions
        )

        private val k2JVMCompilerArgumentsArrayArgumentProperties = commonCompilerArgumentsArrayProperties + listOf(
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
        private val k2MetadataCompilerArgumentsArrayProperties = commonCompilerArgumentsArrayProperties + listOf(
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
            K2JSCompilerArguments::irDcePrintReachabilityInfo,
            K2JSCompilerArguments::irPropertyLazyInitialization,
            K2JSCompilerArguments::irOnly,
            K2JSCompilerArguments::irPerModule,
            K2JSCompilerArguments::generateDts,
            K2JSCompilerArguments::typedArrays,
            K2JSCompilerArguments::friendModulesDisabled,
            K2JSCompilerArguments::metadataOnly,
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
            K2JSCompilerArguments::irDceRuntimeDiagnostic,
            K2JSCompilerArguments::repositries,
        )
        private val k2JSCompilerArgumentsArrayProperties = commonCompilerArgumentsArrayProperties

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

        private fun assertContentEquals(expect: Collection<KProperty<*>>, actual: Collection<KProperty<*>>) {
            //assert(expect.count() == actual.count()) {
            //    "Expected arguments count \"${expect.count()}\" doesn't match with actual \"${actual.count()}\"!"
            //}
            val processor: (Collection<KProperty<*>>) -> Collection<String> = { el -> el.map { it.name }.sorted() }
            assertEquals(
                processor(expect).joinToString("\n"),
                processor(actual).joinToString("\n")
            )
        }
    }
}
