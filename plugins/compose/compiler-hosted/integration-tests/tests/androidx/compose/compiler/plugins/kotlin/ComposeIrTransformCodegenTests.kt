/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.k2.ComposeFirExtensionRegistrar
import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.FULL_JDK
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

private const val COMPOSE_IR_TRANSFORM_TEST_DATA_PATH =
    "plugins/compose/compiler-hosted/integration-tests/testData/codegen/irTransform"

private val NON_IR_TRANSFORM_DUMP_DIRECTORIES = setOf(
    "ComposerParamSignatureTests",
    "FunctionKeyMetaAnnotationsTests",
    "GroupAnalysisCompilerTest",
)

open class AbstractComposeIrTransformCodegenTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        setupJvmPipelineSteps(FirParser.LightTree)
        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }
        defaultDirectives {
            +WITH_STDLIB
            +FULL_JDK
        }
        useConfigurators(
            ::ComposeIrTransformEnvironmentConfigurator,
            ::ComposeIrTransformClasspathConfigurator,
        )
        useAdditionalSourceProviders(::ComposeIrTransformHelpersProvider)
        configureIrHandlersStep {
            useHandlers(::ComposeIrTransformDumpHandler)
        }
        useFailureSuppressors(::BlackBoxCodegenSuppressor)
        enableMetaInfoHandler()
    }
}

class ComposeIrTransformCodegenTests : AbstractComposeIrTransformCodegenTest() {
    @TestFactory
    fun testIrTransformData(): Stream<DynamicTest> {
        val root = File(COMPOSE_IR_TRANSFORM_TEST_DATA_PATH)
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.name.endsWith(".extra.kt") && !it.name.endsWith(".dependency.kt") }
            .filter { it.relativeTo(root).invariantSeparatorsPath.substringBefore("/") !in NON_IR_TRANSFORM_DUMP_DIRECTORIES }
            .sortedBy { it.invariantSeparatorsPath }
            .map { file ->
                DynamicTest.dynamicTest(file.relativeTo(root).invariantSeparatorsPath) {
                    runTest(file.invariantSeparatorsPath)
                }
            }
            .asStream()
    }
}

private class ComposeIrTransformDumpHandler(
    testServices: TestServices,
) : AbstractIrHandler(testServices) {
    private val actualDumps = linkedMapOf<File, String>()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        val testFile = module.files.singleOrNull { !it.isAdditional && it.name.endsWith(".kt") } ?: return
        val irFile = info.irModuleFragment.files.singleOrNull { it.correspondsTo(testFile) }
            ?: info.irModuleFragment.files.firstOrNull()
            ?: return
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().resolveSibling(
            testServices.moduleStructure.originalTestDataFiles.first().nameWithoutExtension + ".txt"
        )
        actualDumps[expectedFile] = irFile.normalizedComposeDump(testFile.originalContent)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        for (actualDump in actualDumps) {
            assertions.assertEqualsToFile(actualDump.key, actualDump.value)
        }
    }
}

private fun IrFile.correspondsTo(testFile: org.jetbrains.kotlin.test.model.TestFile): Boolean =
    fileEntry.name.endsWith(testFile.relativePath) || File(fileEntry.name).name == testFile.name

private fun IrFile.normalizedComposeDump(source: String): String {
    val keySet = mutableListOf<Int>()
    val normalizedSource = source.removeAppendedUsedHelper()
    return dumpSrc(true)
        .replace('$', '%')
        .replace(
            Regex("(%composer\\.start(Restart|Movable|Replaceable|Replace)Group\\()-?((0b)?[-\\d]+)")
        ) {
            val stringKey = it.groupValues[3]
            val key = if (stringKey.startsWith("0b")) {
                Integer.parseInt(stringKey.drop(2), 2)
            } else {
                stringKey.toInt()
            }
            if (key in keySet) {
                "${it.groupValues[1]}<!DUPLICATE KEY: $key!>"
            } else {
                keySet.add(key)
                "${it.groupValues[1]}<>"
            }
        }
        .replace(Regex("(sourceInformationMarkerStart\\(%composer, )([-\\d]+)")) {
            "${it.groupValues[1]}<>"
        }
        .replace(Regex("traceEventStart\\(-?\\d+, (%dirty|%changed|-1), (%dirty1|%changed1|-1), (.*)")) {
            "traceEventStart(<>, ${it.groupValues[1]}, ${it.groupValues[2]}, <>)"
        }
        .replace(
            Regex(
                "(%composer\\.start(Restart|Movable|Replaceable|Replace)" +
                        "Group\\(([^\"\\n]*)\"(.*)\"\\))"
            )
        ) {
            "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[4], normalizedSource)}\")"
        }
        .replace(Regex("(sourceInformation(MarkerStart)?\\(.*)\"(.*)\"\\)")) {
            "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[3], normalizedSource)}\")"
        }
        .replace(
            Regex(
                "(composableLambdaN?\\(([^\"\\n]*)\"(.*)\"\\))"
            )
        ) {
            "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[2], normalizedSource)}\")"
        }
        .replace(Regex("(rememberComposableLambdaN?)\\((-?\\d+)")) {
            "${it.groupValues[1]}(<>"
        }
        .replace(Regex("(%composer\\.joinKey\\()([-\\d]+)")) {
            "${it.groupValues[1]}<>"
        }
        .replace(Regex("(composableLambdaInstance\\()([-\\d]+, (true|false))")) {
            "${it.groupValues[1]}<>, ${it.groupValues[3]}"
        }
        .replace(Regex("(composableLambda\\(%composer,\\s)([-\\d]+)")) {
            "${it.groupValues[1]}<>"
        }
        .removeDumpedUsedHelper()
        .replace(Regex(":${Regex.escape(File(fileEntry.name).name)}(?=[)#\"])"), ":Test.kt")
        .normalizeGeneratedTestFileNames()
        .trimIndent()
        .trimTrailingWhitespacesAndAddNewlineAtEOF()
}

private fun String.normalizeGeneratedTestFileNames(): String =
    replace(Regex("ComposableSingletons%[^\\s.]+Kt"), "ComposableSingletons%TestKt")
        .replace(Regex("\\blambda%-?\\d+"), "lambda%<>")
        .replace(Regex("LiveLiterals%[^\\s.]+Kt"), "LiveLiterals%TestKt")
        .replace(Regex("@LiveLiteralFileInfo\\(file = \"[^\"]+\"\\)"), "@LiveLiteralFileInfo(file = \"/Test.kt\")")
        .replace(Regex("\\b(?:a|dependency)\\.(Wrapper|Foo|Bar|AClass)\\.%stable"), "$1.%stable")

private fun String.removeAppendedUsedHelper(): String =
    replace(Regex("\\n\\s*fun used\\(x: Any\\?\\) \\{}\\s*$"), "")

private fun String.removeDumpedUsedHelper(): String =
    replace(Regex("\\nfun used\\(x: Any\\?\\) \\{\\s*}\\s*"), "\n")

private class ComposeIrTransformHelpersProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<org.jetbrains.kotlin.test.model.TestFile> {
        val packageName = module.files.firstNotNullOfOrNull { file ->
            Regex("^\\s*package\\s+([\\w.]+)", RegexOption.MULTILINE)
                .find(file.originalContent)
                ?.groupValues
                ?.get(1)
        }
        val suiteName = testModuleStructure.originalTestDataFiles.first().parentFile.name
        val testDataFile = testModuleStructure.originalTestDataFiles.first()
        val sidecarSourceFiles = testDataFile.sidecarSourceFiles()
        val source = buildString {
            append(module.files.joinToString("\n") { it.originalContent })
            sidecarSourceFiles.forEach {
                appendLine()
                append(it.readText())
            }
        }
        val content = buildString {
            if (packageName != null) {
                appendLine("package $packageName")
                appendLine()
            }
            appendLine(composeIrTransformHelpersFor(source, suiteName))
        }
        val helperFile = testServices.temporaryDirectoryManager
            .getOrCreateTempDirectory("composeIrTransformHelpers")
            .resolve("ComposeIrTransformHelpers_${module.name}_${packageName ?: "root"}.kt")
            .also { it.writeText(content) }
        return buildList {
            sidecarSourceFiles.forEach { add(it.toTestFile()) }
            if (sidecarSourceFiles.isEmpty()) {
                add(helperFile.toTestFile())
            }
            if (sidecarSourceFiles.isEmpty() && source.contains(Regex("^\\s*import\\s+a\\.\\*", RegexOption.MULTILINE))) {
                add(writePackageHelper("a", "ComposeIrTransformHelpers_${module.name}_a.kt").toTestFile())
            }
            if (sidecarSourceFiles.isEmpty() && source.contains(Regex("^\\s*import\\s+dependency\\.\\*", RegexOption.MULTILINE))) {
                add(writePackageHelper("dependency", "ComposeIrTransformHelpers_${module.name}_dependency.kt").toTestFile())
            }
        }
    }

    private fun writePackageHelper(packageName: String, fileName: String): File {
        val content = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine(COMPOSE_IR_TRANSFORM_PACKAGE_HELPERS)
        }
        return testServices.temporaryDirectoryManager
            .getOrCreateTempDirectory("composeIrTransformHelpers")
            .resolve(fileName)
            .also { it.writeText(content) }
    }
}

private fun File.sidecarSourceFiles(): List<File> =
    (exactSidecarSourceFiles() + parameterizedFallbackSidecarSourceFiles()).distinct().filter { it.exists() }

private fun File.exactSidecarSourceFiles(): List<File> = listOf(
    resolveSibling("$nameWithoutExtension.extra.kt"),
    resolveSibling("$nameWithoutExtension.dependency.kt"),
)

private fun File.parameterizedFallbackSidecarSourceFiles(): List<File> {
    val baseName = nameWithoutExtension.substringBefore("[")
    if (baseName == nameWithoutExtension) return emptyList()
    return listOf(
        resolveSibling("$baseName.extra.kt"),
        resolveSibling("$baseName.dependency.kt"),
    ).filter { it.exists() }
}

private fun composeIrTransformHelpersFor(source: String, suiteName: String): String = buildString {
    appendLine(COMPOSE_IR_TRANSFORM_HELPER_IMPORTS)
    appendLine()
    val importedFallbackPackage = source.contains(Regex("^\\s*import\\s+(a|dependency)\\.\\*", RegexOption.MULTILINE))
    val skippedNames = if (importedFallbackPackage) setOf("A", "B", "Wrapper", "Foo", "Bar") else emptySet()
    val declarations = COMPOSE_IR_TRANSFORM_HELPER_DECLARATIONS + suiteSpecificHelperDeclarations(suiteName)
    for (entry in declarations) {
        if (entry.first in skippedNames) continue
        if (!source.declaresTopLevel(entry.first)) {
            appendLine(entry.second)
        }
    }
}.trim()

private fun String.declaresTopLevel(name: String): Boolean =
    contains(Regex("\\b(class|interface|object|fun|val|var|typealias)\\s+$name\\b"))

private val COMPOSE_IR_TRANSFORM_HELPER_IMPORTS = """
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.ReadOnlyComposable
    import androidx.compose.runtime.Stable
    import androidx.compose.runtime.mutableStateOf
    import kotlin.reflect.KProperty
""".trimIndent()

private val COMPOSE_IR_TRANSFORM_HELPER_DECLARATIONS = listOf(
    "use" to "fun use(x: Any?) {}",
    "StableClass" to "@Stable class StableClass",
    "Unstable" to "class Unstable(var value: Int = 0)",
    "StableDelegate" to """
        class StableDelegate {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0
            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
        }
    """.trimIndent(),
    "EmptyClass" to "class EmptyClass",
    "Data" to "class Data(val value: String = \"\")",
    "NullableData" to "inline class NullableData(val value: String?)",
    "IntData" to "inline class IntData(val value: Int)",
    "Foo" to "class Foo",
    "Bar" to "class Bar",
    "Cls" to "class Cls",
    "InlineInt" to "class InlineInt(val value: Int)",
    "InlineClass" to "inline class InlineClass(val value: Int)",
    "UnstableDelegate" to """
        class UnstableDelegate {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0
            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
        }
    """.trimIndent(),
    "A" to """
        @Composable fun A(x: Any? = null, y: Any? = null, z: Any? = null, w: Any? = null, v: Any? = null) {}
    """.trimIndent(),
    "B" to """
        @Composable fun B(): Boolean = true
        @Composable fun B(x: Any? = null, y: Any? = null, z: Any? = null, w: Any? = null): Boolean = true
    """.trimIndent(),
    "R" to """
        @Composable fun R(): Int = 10
        @Composable fun R(x: Int): Int = 10
    """.trimIndent(),
    "P" to "@Composable fun P(x: Int) {}",
    "Text" to "@Composable fun Text(text: String = \"\", vararg values: Any?) {}",
    "Button" to "@Composable fun Button(onClick: () -> Unit = {}, content: @Composable () -> Unit = {}) { content() }",
    "Wrapper" to """
        class Wrapper<T>(val value: T)
        @Composable fun Wrapper(content: @Composable () -> Unit) { content() }
    """.trimIndent(),
    "Wrap" to "@Composable fun Wrap(content: @Composable () -> Unit) { content() }",
    "W" to "@Composable fun W(content: @Composable () -> Unit) { content() }",
    "Box" to "@Composable fun Box(content: @Composable () -> Unit = {}) { content() }",
    "Circle" to "@Composable fun Circle() {}",
    "Layout" to "@Composable fun Layout(content: @Composable () -> Unit = {}) { content() }",
    "Display" to "@Composable fun Display(value: Any?) {}",
    "Leaf" to "@Composable fun Leaf(vararg values: Any?, content: @Composable () -> Unit = {}) { content() }",
    "Call" to "@Composable fun Call(content: @Composable () -> Unit = {}) { content() }",
    "MaterialTheme" to "object MaterialTheme",
    "Modifier" to "object Modifier",
    "ContentImpl" to "@Composable fun ContentImpl() {}",
    "SomeClass" to "class SomeClass",
    "C" to "@Composable fun C(vararg values: Any?) {}",
    "someComposableValue" to "@Composable fun someComposableValue(): Any? = null",
    "compositionLocalBar" to """
        object compositionLocalBar {
            val current: Any?
                @Composable get() = null
        }
    """.trimIndent(),
    "createFactory" to "fun <T> createFactory(value: T): () -> T = { value }",
    "scope" to "@Composable inline fun <T> scope(block: @Composable () -> T): T = block()",
    "IW" to "@Composable inline fun IW(content: @Composable () -> Unit) { content() }",
    "NA" to "fun NA() {}",
    "NB" to "fun NB(): Boolean = true",
    "NR" to "fun NR(): Int = 10",
    "makeInt" to "fun makeInt(): Int = 1",
    "newInt" to "fun newInt(): Int = 1",
    "getCondition" to "fun getCondition(): Boolean = true",
    "getConditionA" to "fun getConditionA(): Boolean = true",
    "thenIf" to "fun thenIf(condition: Boolean, block: () -> Unit) { if (condition) block() }",
    "a" to "var a = 1",
    "b" to "var b = 2",
    "c" to "var c = 3",
    "someInt" to "val someInt = 1",
)

private fun suiteSpecificHelperDeclarations(suiteName: String): List<Pair<String, String>> = when (suiteName) {
    "ControlFlowTransformTests", "ControlFlowTransformTestsNoSource" -> listOf(
        "L" to "@Composable fun L(): List<Int> = listOf(1, 2, 3)",
    )
    else -> emptyList()
}

private val COMPOSE_IR_TRANSFORM_PACKAGE_HELPERS = """
    import androidx.compose.runtime.Composable

    class Wrapper<T>(val value: T)
    class Foo
    class Bar
    class AClass
    @Composable fun A(y: Any?) {}
    @Composable fun B(y: Any?) {}
    fun used(x: Any?) {}
""".trimIndent()

private fun generateSourceInfo(sourceInfo: String, source: String): String {
    val r = Regex("(\\d+)|(,)|([*])|(:)|C(\\(.*\\))?|L|(P\\(*\\))|@")
    var current = 0
    var currentResult = r.find(sourceInfo, current)
    var result = ""

    fun MatchResult.isNumber() = groupValues[1].isNotEmpty()
    fun MatchResult.number() = groupValues[1].toInt()
    fun MatchResult.text() = groupValues[0]
    fun MatchResult.isChar(c: String) = text() == c
    fun MatchResult.isFileName() = groups[4] != null

    fun next(): MatchResult? {
        currentResult?.let {
            current = it.range.last + 1
            currentResult = it.next()
        }
        return currentResult
    }

    fun parseLocation(): String? {
        var mr = currentResult
        if (mr != null && mr.isNumber()) {
            mr = next()
        }
        if (mr != null && mr.isChar("@")) {
            mr = next()
            if (mr == null || !mr.isNumber()) return null
            val offset = mr.number()
            mr = next()
            var ellipsis = ""
            val maxFragment = 6
            val rawLength = if (mr != null && mr.isChar("L")) {
                mr = next()
                if (mr == null || !mr.isNumber()) return null
                mr.number().also { next() }
            } else {
                maxFragment
            }
            val eol = source.indexOf('\n', offset).let { if (it < 0) source.length else it }
            val space = source.indexOf(' ', offset).let { if (it < 0) source.length else it }
            val maxEnd = offset + maxFragment
            if (eol > maxEnd && space > maxEnd) ellipsis = "..."
            val length = minOf(maxEnd, minOf(offset + rawLength, space, eol)) - offset
            return "<${source.substring(offset, offset + length)}$ellipsis>"
        }
        return null
    }

    while (currentResult != null) {
        val mr = currentResult!!
        if (mr.range.first != current) {
            return "invalid source info at $current: '$sourceInfo'"
        }
        when {
            mr.isNumber() || mr.isChar("@") -> {
                val fragment = parseLocation() ?: return "invalid source info at $current: '$sourceInfo'"
                result += fragment
            }
            mr.isFileName() -> {
                return result + ":" + sourceInfo.substring(mr.range.last + 1)
            }
            else -> {
                result += mr.text()
                next()
            }
        }
        require(mr != currentResult) { "regex didn't advance" }
    }
    if (current != sourceInfo.length) {
        return "invalid source info at $current: '$sourceInfo'"
    }
    return result
}

private class ComposeIrTransformEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    @OptIn(ExperimentalCompilerApi::class)
    override fun ExtensionStorage.registerCompilerExtensions(module: TestModule, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(ComposeFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(ComposePluginRegistrar.createComposeIrExtension(configuration))
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val suiteName = testDataFile.parentFile.name
        val fileName = testDataFile.nameWithoutExtension

        configuration.put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, suiteName !in SOURCE_INFORMATION_DISABLED_SUITES)
        configuration.put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, suiteName !in TRACE_MARKERS_DISABLED_SUITES)
        if (suiteName in LIVE_LITERAL_SUITES) {
            configuration.put(ComposeConfiguration.LIVE_LITERALS_ENABLED_KEY, true)
        }
        if (suiteName in LIVE_LITERAL_V2_SUITES) {
            configuration.put(ComposeConfiguration.LIVE_LITERALS_V2_ENABLED_KEY, true)
        }
        if (suiteName in CONTEXT_PARAMETERS_SUITES) {
            configuration.languageVersionSettings = LanguageVersionSettingsImpl(
                languageVersion = configuration.languageVersionSettings.languageVersion,
                apiVersion = configuration.languageVersionSettings.apiVersion,
                analysisFlags = mapOf(
                    AnalysisFlags.allowUnstableDependencies to true,
                    AnalysisFlags.skipPrereleaseCheck to true,
                ),
                specificFeatures = mapOf(
                    LanguageFeature.ContextParameters to LanguageFeature.State.ENABLED,
                )
            )
        }
        if (suiteName == "ComposeRuntimeTargetTests") {
            configuration.put(ComposeConfiguration.TARGET_RUNTIME_VERSION_KEY, ComposeRuntimeVersion.v1_8.value)
        }
        if (suiteName == "SingleStabilityConfigurationTest") {
            configuration.put(
                ComposeConfiguration.STABILITY_CONFIG_PATH_KEY,
                listOf("plugins/compose/compiler-hosted/integration-tests/testResources/testStabilityConfigFiles/config1.conf")
            )
            configuration.put(ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY, false)
        }
        if (suiteName == "MultipleStabilityConfigurationTest") {
            configuration.put(
                ComposeConfiguration.STABILITY_CONFIG_PATH_KEY,
                listOf(
                    "plugins/compose/compiler-hosted/integration-tests/testResources/testStabilityConfigFiles/config1.conf",
                    "plugins/compose/compiler-hosted/integration-tests/testResources/testStabilityConfigFiles/config2.conf",
                )
            )
        }
        configuration.put(ComposeConfiguration.FEATURE_FLAGS, featureFlagsFor(suiteName, fileName))
    }
}

private val SOURCE_INFORMATION_DISABLED_SUITES = setOf(
    "ControlFlowTransformTestsNoSource",
    "FunctionBodySkippingTransformTestsNoSource",
    "ComposePausableCompositionTests",
    "MultipleStabilityConfigurationTest",
    "SingleStabilityConfigurationTest",
    "StrongSkippingModeTransformTests",
)

private val TRACE_MARKERS_DISABLED_SUITES = setOf(
    "ControlFlowTransformTestsNoSource",
    "FunctionBodySkippingTransformTestsNoSource",
    "ComposePausableCompositionTests",
)
private val LIVE_LITERAL_SUITES = setOf("LiveLiteralTransformTests")
private val LIVE_LITERAL_V2_SUITES = setOf("LiveLiteralV2TransformTests")
private val CONTEXT_PARAMETERS_SUITES = setOf(
    "ContextParametersTransformTests",
    "LambdaMemoizationTransformTests",
)

private fun featureFlagsFor(suiteName: String, fileName: String): List<String> = when (suiteName) {
    "RememberIntrinsicTransformTests" -> listOf(
        FeatureFlag.OptimizeNonSkippingGroups.featureName,
        FeatureFlag.IntrinsicRemember.featureName,
    )
    "RememberIntrinsicTransformTestsStrongSkipping" -> listOf(
        FeatureFlag.IntrinsicRemember.featureName,
        FeatureFlag.StrongSkipping.featureName,
    )
    "StrongSkippingModeTransformTests" -> listOf(
        FeatureFlag.StrongSkipping.featureName,
        FeatureFlag.OptimizeNonSkippingGroups.featureName,
        FeatureFlag.IntrinsicRemember.name(fileName.contains("intrinsicRemember = true")),
    )
    "OptimizeNonSkippingGroupsTests" -> listOf(
        FeatureFlag.OptimizeNonSkippingGroups.name(!fileName.contains("optimizeNonSkippingGroups = false")),
    )
    "ComposePausableCompositionTests" -> listOf(
        FeatureFlag.PausableComposition.name(fileName.contains("pausableEnabled = true")),
    )
    "MultipleStabilityConfigurationTest" -> listOf(
        FeatureFlag.OptimizeNonSkippingGroups.featureName,
    )
    "SingleStabilityConfigurationTest" -> emptyList()
    "ControlFlowTransformTestsNoSource", "FunctionBodySkippingTransformTestsNoSource" -> listOf(
        FeatureFlag.OptimizeNonSkippingGroups.featureName,
    )
    else -> listOf(
        FeatureFlag.StrongSkipping.featureName,
        FeatureFlag.OptimizeNonSkippingGroups.featureName,
    )
}

private class ComposeIrTransformClasspathConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val platform = module.targetPlatform(testServices)
        check(platform.isJvm()) {
            "Compose IR transform tests support only JVM"
        }
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_11)
        composeLibraries.forEach(configuration::addJvmClasspathRoot)
    }
}

private val composeLibraries by lazy {
    val classPath = System.getProperty("java.class.path") ?: error("System property \"java.class.path\" is not found")
    classPath.split(File.pathSeparator).map(::File).filter {
        val path = it.absolutePath
        path.contains("androidx.compose") || path.contains("kotlinx-coroutines")
    } + File("plugins/compose/compiler-hosted/integration-tests/protobuf-test-classes/build/libs")
        .listFiles()
        .orEmpty()
        .filter { it.name.startsWith("protobuf-test-classes") && it.extension == "jar" }
}
