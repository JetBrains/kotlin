/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots

@Suppress("LeakingThis")
abstract class ComposeIrTransformTest : AbstractIrTransformTest() {
    open val liveLiteralsEnabled get() = false
    open val liveLiteralsV2Enabled get() = false
    open val generateFunctionKeyMetaClasses get() = false
    open val sourceInformationEnabled get() = true
    open val intrinsicRememberEnabled get() = false
    open val decoysEnabled get() = false
    open val metricsDestination: String? get() = null

    protected var extension: ComposeIrGenerationExtension? = null
    // Some tests require the plugin context in order to perform assertions, for example, a
    // context is required to determine the stability of a type using the StabilityInferencer.
    var pluginContext: IrPluginContext? = null

    override fun setUp() {
        super.setUp()
        extension = ComposeIrGenerationExtension(
            myEnvironment!!.configuration,
            liveLiteralsEnabled,
            liveLiteralsV2Enabled,
            generateFunctionKeyMetaClasses,
            sourceInformationEnabled,
            intrinsicRememberEnabled,
            decoysEnabled,
            metricsDestination
        )
    }

    override fun postProcessingStep(
        module: IrModuleFragment,
        context: IrPluginContext
    ) {
        pluginContext = context
        extension!!.generate(
            module,
            context
        )
    }

    override fun tearDown() {
        pluginContext = null
        extension = null
        super.tearDown()
    }
}

abstract class AbstractIrTransformTest : AbstractCodegenTest() {
    private var testLocalUnique = 0
    protected var classesDirectory = tmpDir(
        "kotlin-${testLocalUnique++}-classes"
    )
    override val additionalPaths: List<File> = listOf(classesDirectory)

    abstract fun postProcessingStep(
        module: IrModuleFragment,
        context: IrPluginContext,
    )

    fun verifyCrossModuleComposeIrTransform(
        @Language("kotlin")
        dependencySource: String,
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false,
        dumpClasses: Boolean = false,
    ) {
        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        val dependencyFileName = "Test_REPLACEME_${uniqueNumber++}"

        classLoader(dependencySource, dependencyFileName, dumpClasses)
            .allGeneratedFiles
            .also {
                // Write the files to the class directory so they can be used by the next module
                // and the application
                it.writeToDir(classesDirectory)
            }

        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        verifyComposeIrTransform(
            source,
            expectedTransformed,
            "",
            dumpTree = dumpTree
        )
    }

    fun verifyComposeIrTransform(
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
        @Language("kotlin")
        extra: String = "",
        validator: (element: IrElement) -> Unit = { },
        dumpTree: Boolean = false,
        truncateTracingInfoMode: TruncateTracingInfoMode = TruncateTracingInfoMode.TRUNCATE_ALL,
        compilation: Compilation = JvmCompilation()
    ) {
        if (!compilation.enabled) {
            // todo indicate ignore?
            return
        }

        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$')),
            sourceFile("Extra.kt", extra.replace('%', '$'))
        )
        val irModule = compilation.compile(files)
        val keySet = mutableListOf<Int>()
        fun IrElement.validate(): IrElement = this.also { validator(it) }
        val actualTransformed = irModule
            .files[0]
            .validate()
            .dumpSrc()
            .replace('$', '%')
            // replace source keys for start group calls
            .replace(
                Regex(
                    "(%composer\\.start(Restart|Movable|Replaceable)Group\\()-?((0b)?[-\\d]+)"
                )
            ) {
                val stringKey = it.groupValues[3]
                val key = if (stringKey.startsWith("0b"))
                    Integer.parseInt(stringKey.drop(2), 2)
                else
                    stringKey.toInt()
                if (key in keySet) {
                    "${it.groupValues[1]}<!DUPLICATE KEY: $key!>"
                } else {
                    keySet.add(key)
                    "${it.groupValues[1]}<>"
                }
            }
            .replace(
                Regex("(sourceInformationMarkerStart\\(%composer, )([-\\d]+)")
            ) {
                "${it.groupValues[1]}<>"
            }
            // replace traceEventStart values with a token
            // TODO(174715171): capture actual values for testing
            .replace(
                Regex("traceEventStart\\(-?\\d+, (-?\\d+, -?\\d+), (.*)")
            ) {
                when (truncateTracingInfoMode) {
                    TruncateTracingInfoMode.TRUNCATE_ALL ->
                        "traceEventStart(<>)"
                    TruncateTracingInfoMode.TRUNCATE_KEY ->
                        "traceEventStart(<>, ${it.groupValues[1]}, ${it.groupValues[2]}"
                    TruncateTracingInfoMode.KEEP_INFO_STRING ->
                        "traceEventStart(<>, ${it.groupValues[2]}"
                }
            }
            // replace source information with source it references
            .replace(
                Regex(
                    "(%composer\\.start(Restart|Movable|Replaceable)Group\\" +
                        "([^\"\\n]*)\"(.*)\"\\)"
                )
            ) {
                "${it.groupValues[1]}\"${
                generateSourceInfo(it.groupValues[4], source)
                }\")"
            }
            .replace(
                Regex("(sourceInformation(MarkerStart)?\\(.*)\"(.*)\"\\)")
            ) {
                "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[3], source)}\")"
            }
            .replace(
                Regex(
                    "(composableLambda[N]?\\" +
                        "([^\"\\n]*)\"(.*)\"\\)"
                )
            ) {
                "${it.groupValues[1]}\"${
                generateSourceInfo(it.groupValues[2], source)
                }\")"
            }
            // replace source keys for joinKey calls
            .replace(
                Regex(
                    "(%composer\\.joinKey\\()([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            // composableLambdaInstance(<>, true)
            .replace(
                Regex(
                    "(composableLambdaInstance\\()([-\\d]+, (true|false))"
                )
            ) {
                val callStart = it.groupValues[1]
                val tracked = it.groupValues[3]
                "$callStart<>, $tracked"
            }
            // composableLambda(%composer, <>, true)
            .replace(
                Regex(
                    "(composableLambda\\(%composer,\\s)([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            .trimIndent()
            .trimTrailingWhitespacesAndAddNewlineAtEOF()

        if (dumpTree) {
            println(irModule.dump())
        }
        assertEquals(
            expectedTransformed
                .trimIndent()
                .trimTrailingWhitespacesAndAddNewlineAtEOF(),
            actualTransformed
        )
    }

    private fun MatchResult.isNumber() = groupValues[1].isNotEmpty()
    private fun MatchResult.number() = groupValues[1].toInt()
    private val MatchResult.text get() = groupValues[0]
    private fun MatchResult.isChar(c: String) = text == c
    private fun MatchResult.isFileName() = groups[4] != null

    private fun generateSourceInfo(sourceInfo: String, source: String): String {
        val r = Regex("(\\d+)|([,])|([*])|([:])|C(\\(.*\\))?|L|(P\\(*\\))|@")
        var current = 0
        var currentResult = r.find(sourceInfo, current)
        var result = ""

        fun next(): MatchResult? {
            currentResult?.let {
                current = it.range.last + 1
                currentResult = it.next()
            }
            return currentResult
        }

        // A location has the format: [<line-number>]['@' <offset> ['L' <length>]]
        // where the named productions are numbers
        fun parseLocation(): String? {
            var mr = currentResult
            if (mr != null && mr.isNumber()) {
                // line number, we ignore the value in during testing.
                mr = next()
            }
            if (mr != null && mr.isChar("@")) {
                // Offset
                mr = next()
                if (mr == null || !mr.isNumber()) {
                    return null
                }
                val offset = mr.number()
                mr = next()
                var ellipsis = ""
                val maxFragment = 6
                val rawLength = if (mr != null && mr.isChar("L")) {
                    mr = next()
                    if (mr == null || !mr.isNumber()) {
                        return null
                    }
                    mr.number().also { next() }
                } else {
                    maxFragment
                }
                val eol = source.indexOf('\n', offset).let {
                    if (it < 0) source.length else it
                }
                val space = source.indexOf(' ', offset).let {
                    if (it < 0) source.length else it
                }
                val maxEnd = offset + maxFragment
                if (eol > maxEnd && space > maxEnd) ellipsis = "..."
                val length = minOf(maxEnd, minOf(offset + rawLength, space, eol)) - offset
                return "<${source.substring(offset, offset + length)}$ellipsis>"
            }
            return null
        }

        while (currentResult != null) {
            val mr = currentResult!!
            if (mr.range.start != current) {
                return "invalid source info at $current: '$sourceInfo'"
            }
            when {
                mr.isNumber() || mr.isChar("@") -> {
                    val fragment = parseLocation()
                        ?: return "invalid source info at $current: '$sourceInfo'"
                    result += fragment
                }
                mr.isFileName() -> {
                    return result + ":" + sourceInfo.substring(mr.range.last + 1)
                }
                else -> {
                    result += mr.text
                    next()
                }
            }
            require(mr != currentResult) { "regex didn't advance" }
        }
        if (current != sourceInfo.length)
            return "invalid source info at $current: '$sourceInfo'"
        return result
    }

    fun facadeClassGenerator(
        generatorContext: GeneratorContext,
        source: DeserializedContainerSource
    ): IrClass? {
        val jvmPackagePartSource = source.safeAs<JvmPackagePartSource>() ?: return null
        val facadeName = jvmPackagePartSource.facadeClassName ?: jvmPackagePartSource.className
        return generatorContext.irFactory.buildClass {
            origin = IrDeclarationOrigin.FILE_CLASS
            name = facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.also {
            it.createParameterDeclarations()
        }
    }

    inner class JvmCompilation(
        private val specificFeature: Set<LanguageFeature> = emptySet()
    ) : Compilation {
        override val enabled: Boolean = true

        override fun compile(files: List<KtFile>): IrModuleFragment {
            val classPath = createClasspath() + additionalPaths
            val configuration = newConfiguration()
            configuration.addJvmClasspathRoots(classPath)
            configuration.put(JVMConfigurationKeys.IR, true)
            configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
            configuration.languageVersionSettings =
                configuration.languageVersionSettings.setFeatures(specificFeature)

            configuration.configureJdkClasspathRoots()

            val environment = KotlinCoreEnvironment.createForTests(
                myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
            ).also { setupEnvironment(it) }

            val mangler = JvmDescriptorMangler(null)

            val psi2ir = Psi2IrTranslator(
                environment.configuration.languageVersionSettings,
                Psi2IrConfiguration(ignoreErrors = false)
            )
            val messageLogger = environment.configuration[IrMessageLogger.IR_MESSAGE_LOGGER]
                ?: IrMessageLogger.None
            val symbolTable = SymbolTable(
                JvmIdSignatureDescriptor(mangler),
                IrFactoryImpl
            )

            val analysisResult = JvmResolveUtil.analyze(files, environment)
            if (!psi2ir.configuration.ignoreErrors) {
                analysisResult.throwIfError()
                AnalyzingUtils.throwExceptionOnErrors(analysisResult.bindingContext)
            }
            val extensions = JvmGeneratorExtensionsImpl(configuration)
            val generatorContext = psi2ir.createGeneratorContext(
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                symbolTable,
                extensions = extensions
            )
            val stubGenerator = DeclarationStubGeneratorImpl(
                generatorContext.moduleDescriptor,
                generatorContext.symbolTable,
                generatorContext.irBuiltIns,
                DescriptorByIdSignatureFinderImpl(generatorContext.moduleDescriptor, mangler),
                extensions
            )
            val frontEndContext = object : TranslationPluginContext {
                override val moduleDescriptor: ModuleDescriptor
                    get() = generatorContext.moduleDescriptor
                override val symbolTable: ReferenceSymbolTable
                    get() = symbolTable
                override val typeTranslator: TypeTranslator
                    get() = generatorContext.typeTranslator
                override val irBuiltIns: IrBuiltIns
                    get() = generatorContext.irBuiltIns
            }
            val irLinker = JvmIrLinker(
                generatorContext.moduleDescriptor,
                messageLogger,
                JvmIrTypeSystemContext(generatorContext.irBuiltIns),
                generatorContext.symbolTable,
                frontEndContext,
                stubGenerator,
                mangler,
                true
            )

            generatorContext.moduleDescriptor.allDependencyModules.map {
                val capability = it.getCapability(KlibModuleOrigin.CAPABILITY)
                val kotlinLibrary = (capability as? DeserializedKlibModuleOrigin)?.library
                irLinker.deserializeIrModuleHeader(
                    it,
                    kotlinLibrary,
                    _moduleName = it.name.asString()
                )
            }

            val irProviders = listOf(irLinker)

            val symbols = BuiltinSymbolsBase(
                generatorContext.irBuiltIns,
                symbolTable,
            )

            ExternalDependenciesGenerator(
                generatorContext.symbolTable,
                irProviders
            ).generateUnboundSymbolsAsDependencies()

            psi2ir.addPostprocessingStep { module ->
                val old = stubGenerator.unboundSymbolGeneration
                try {
                    stubGenerator.unboundSymbolGeneration = true
                    val context = IrPluginContextImpl(
                        module = generatorContext.moduleDescriptor,
                        bindingContext = generatorContext.bindingContext,
                        languageVersionSettings = generatorContext.languageVersionSettings,
                        st = generatorContext.symbolTable,
                        typeTranslator = generatorContext.typeTranslator,
                        irBuiltIns = generatorContext.irBuiltIns,
                        linker = irLinker,
                        symbols = symbols,
                        diagnosticReporter = IrMessageLogger.None,
                    )
                    postProcessingStep(module, context)
                } finally {
                    stubGenerator.unboundSymbolGeneration = old
                }
            }

            val irModuleFragment = psi2ir.generateModuleFragment(
                generatorContext,
                files,
                irProviders,
                IrGenerationExtension.getInstances(myEnvironment!!.project),
                expectDescriptorToSymbol = null
            )
            irLinker.postProcess()
            return irModuleFragment
        }
    }

    // This interface enables different Compilation variants for compiler tests
    interface Compilation {
        val enabled: Boolean
        fun compile(files: List<KtFile>): IrModuleFragment
    }

    enum class TruncateTracingInfoMode {
        TRUNCATE_ALL, // truncates all trace information replacing it with a token
        TRUNCATE_KEY, // truncates only the `key` parameter
        KEEP_INFO_STRING, // truncates everything except for the `info` string
    }
}

internal fun LanguageVersionSettings.setFeatures(
    features: Set<LanguageFeature>
) = LanguageVersionSettingsImpl(
    languageVersion = languageVersion,
    apiVersion = apiVersion,
    specificFeatures = features.associateWith { LanguageFeature.State.ENABLED }
)
