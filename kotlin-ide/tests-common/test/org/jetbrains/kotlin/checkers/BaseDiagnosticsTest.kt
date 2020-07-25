/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.TestFile
import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest.TestModule
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory0
import org.jetbrains.kotlin.checkers.diagnostics.factories.SyntaxErrorDiagnosticFactory
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.junit.Assert
import java.util.regex.Pattern
import kotlin.reflect.jvm.javaField

internal const val JVM_TARGET = "JVM_TARGET"

abstract class BaseDiagnosticsTest : KotlinMultiFileTestWithJava<TestModule, TestFile>() {
    protected lateinit var environment: KotlinCoreEnvironment

    protected val project: Project
        get() = environment.project

    override fun tearDown() {
        this::environment.javaField!![this] = null
        super.tearDown()
    }

    override fun createTestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>
    ): TestModule =
        TestModule(name, dependencies, friends)

    override fun createTestFile(module: TestModule?, fileName: String, text: String, directives: Directives): TestFile =
        TestFile(module, fileName, text, directives)

    class TestModule(name: String, dependencies: List<String>, friends: List<String>) :
        KotlinBaseTest.TestModule(name, dependencies, friends) {
        lateinit var languageVersionSettings: LanguageVersionSettings
    }

    inner class TestFile(
        val module: TestModule?,
        val fileName: String,
        textWithMarkers: String,
        directives: Directives
    ) : KotlinBaseTest.TestFile(fileName, textWithMarkers, directives) {
        private val diagnosedRanges: MutableList<DiagnosedRange> = mutableListOf()
        private val diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>> = mutableMapOf()
        val expectedText: String
        private val clearText: String
        private val createKtFile: Lazy<KtFile?>
        private val whatDiagnosticsToConsider: Condition<Diagnostic>
        private val customLanguageVersionSettings: LanguageVersionSettings?
        val jvmTarget: JvmTarget?
        private val declareCheckType: Boolean = CHECK_TYPE_DIRECTIVE in directives
        private val declareFlexibleType: Boolean
        private val checkLazyLog: Boolean
        private val markDynamicCalls: Boolean
        private val withNewInferenceDirective: Boolean
        private val newInferenceEnabled: Boolean
        private val renderDiagnosticMessages: Boolean
        private val renderDiagnosticsFullText: Boolean

        init {
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives, declareCheckType)
            this.customLanguageVersionSettings = parseLanguageVersionSettings(directives)
            this.jvmTarget = parseJvmTarget(directives)
            this.checkLazyLog = CHECK_LAZY_LOG_DIRECTIVE in directives || CHECK_LAZY_LOG_DEFAULT
            this.declareFlexibleType = EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE in directives
            this.markDynamicCalls = MARK_DYNAMIC_CALLS_DIRECTIVE in directives
            this.withNewInferenceDirective = WITH_NEW_INFERENCE_DIRECTIVE in directives
            this.newInferenceEnabled =
                customLanguageVersionSettings?.supportsFeature(LanguageFeature.NewInference) ?: shouldUseNewInferenceForTests()
            if (fileName.endsWith(".java")) {
                // TODO: check there are no syntax errors in .java sources
                this.createKtFile = lazyOf(null)
                this.clearText = textWithMarkers
                this.expectedText = this.clearText
            } else {
                this.expectedText = textWithMarkers
                this.clearText =
                    CheckerTestUtil.parseDiagnosedRanges(addExtras(expectedText), diagnosedRanges, diagnosedRangesToDiagnosticNames)
                this.createKtFile = lazy { TestCheckerUtil.createCheckAndReturnPsiFile(fileName, clearText, project) }
            }
            this.renderDiagnosticMessages = RENDER_DIAGNOSTICS_MESSAGES in directives
            this.renderDiagnosticsFullText = RENDER_DIAGNOSTICS_FULL_TEXT in directives
        }

        val ktFile: KtFile? by createKtFile

        private val imports: String
            get() = buildString {
                // Line separator is "\n" intentionally here (see DocumentImpl.assertValidSeparators)
                if (declareCheckType) {
                    append(CHECK_TYPE_IMPORT + "\n")
                }
                if (declareFlexibleType) {
                    append(EXPLICIT_FLEXIBLE_TYPES_IMPORT + "\n")
                }
            }

        private val extras: String
            get() = "/*extras*/\n$imports/*extras*/\n\n"

        fun addExtras(text: String): String =
            addImports(text, extras)

        private fun addImports(text: String, imports: String): String {
            var result = text
            val pattern = Pattern.compile("^package [.\\w\\d]*\n", Pattern.MULTILINE)
            val matcher = pattern.matcher(result)
            result = if (matcher.find()) {
                // add imports after the package directive
                result.substring(0, matcher.end()) + imports + result.substring(matcher.end())
            } else {
                // add imports at the beginning
                imports + result
            }
            return result
        }

        private fun shouldUseNewInferenceForTests(): Boolean {
            if (System.getProperty("kotlin.ni") == "true") return true
            return LanguageVersionSettingsImpl.DEFAULT.supportsFeature(LanguageFeature.NewInference)
        }

        override fun toString(): String = ktFile?.name ?: "Java file"
    }

    companion object {
        private const val DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS"
        private val DIAGNOSTICS_PATTERN: Pattern = Pattern.compile("([+\\-!])(\\w+)\\s*")
        private val DIAGNOSTICS_TO_INCLUDE_ANYWAY: Set<DiagnosticFactory<*>> = setOf(
            Errors.UNRESOLVED_REFERENCE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            SyntaxErrorDiagnosticFactory.INSTANCE,
            DebugInfoDiagnosticFactory0.ELEMENT_WITH_ERROR_TYPE,
            DebugInfoDiagnosticFactory0.MISSING_UNRESOLVED,
            DebugInfoDiagnosticFactory0.UNRESOLVED_WITH_TARGET
        )

        const val CHECK_TYPE_DIRECTIVE = "CHECK_TYPE"
        const val CHECK_TYPE_PACKAGE = "tests._checkType"
        const val CHECK_TYPE_IMPORT = "import $CHECK_TYPE_PACKAGE.*"

        const val EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE = "EXPLICIT_FLEXIBLE_TYPES"
        private val EXPLICIT_FLEXIBLE_PACKAGE = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.packageFqName.asString()
        private val EXPLICIT_FLEXIBLE_CLASS_NAME = InternalFlexibleTypeTransformer.FLEXIBLE_TYPE_CLASSIFIER.relativeClassName.asString()
        private val EXPLICIT_FLEXIBLE_TYPES_IMPORT = "import $EXPLICIT_FLEXIBLE_PACKAGE.$EXPLICIT_FLEXIBLE_CLASS_NAME"
        const val CHECK_LAZY_LOG_DIRECTIVE = "CHECK_LAZY_LOG"
        val CHECK_LAZY_LOG_DEFAULT = "true" == System.getProperty("check.lazy.logs", "false")

        const val MARK_DYNAMIC_CALLS_DIRECTIVE = "MARK_DYNAMIC_CALLS"

        const val WITH_NEW_INFERENCE_DIRECTIVE = "WITH_NEW_INFERENCE"

        const val RENDER_DIAGNOSTICS_MESSAGES = "RENDER_DIAGNOSTICS_MESSAGES"

        const val RENDER_DIAGNOSTICS_FULL_TEXT = "RENDER_DIAGNOSTICS_FULL_TEXT"

        val DIAGNOSTIC_IN_TESTDATA_PATTERN = Regex("<!>|<!(.*?(\\(\".*?\"\\)|\\(\\))??)+(?<!<)!>")
        val SPEC_LINKED_TESTDATA_PATTERN =
            Regex("""/\*\s+? \* KOTLIN (PSI|DIAGNOSTICS|CODEGEN BOX) SPEC TEST \((POSITIVE|NEGATIVE)\)\n([\s\S]*?\n)\s+\*/\n""")

        val SPEC_NOT_LINED_TESTDATA_PATTERN =
            Regex("""/\*\s+? \* KOTLIN (PSI|DIAGNOSTICS|CODEGEN BOX) NOT LINKED SPEC TEST \((POSITIVE|NEGATIVE)\)\n([\s\S]*?\n)\s+\*/\n""")


        fun parseDiagnosticFilterDirective(
            directiveMap: Directives,
            allowUnderscoreUsage: Boolean
        ): Condition<Diagnostic> {
            val directives = directiveMap[DIAGNOSTICS_DIRECTIVE]
            val initialCondition =
                if (allowUnderscoreUsage)
                    Condition<Diagnostic> { it.factory.name != "UNDERSCORE_USAGE_WITHOUT_BACKTICKS" }
                else
                    Conditions.alwaysTrue()

            if (directives == null) {
                // If "!API_VERSION" is present, disable the NEWER_VERSION_IN_SINCE_KOTLIN diagnostic.
                // Otherwise it would be reported in any non-trivial test on the @SinceKotlin value.
                if (API_VERSION_DIRECTIVE in directiveMap) {
                    return Conditions.and(initialCondition, Condition { diagnostic ->
                        diagnostic.factory !== Errors.NEWER_VERSION_IN_SINCE_KOTLIN
                    })
                }
                return initialCondition
            }

            var condition = initialCondition
            val matcher = DIAGNOSTICS_PATTERN.matcher(directives)
            if (!matcher.find()) {
                Assert.fail(
                    "Wrong syntax in the '// !$DIAGNOSTICS_DIRECTIVE: ...' directive:\n" +
                            "found: '$directives'\n" +
                            "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                            "where '+' means 'include'\n" +
                            "      '-' means 'exclude'\n" +
                            "      '!' means 'exclude everything but this'\n" +
                            "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR"
                )
            }

            var first = true
            do {
                val operation = matcher.group(1)
                val name = matcher.group(2)

                val newCondition: Condition<Diagnostic> =
                    if (name in setOf("ERROR", "WARNING", "INFO")) {
                        Condition { diagnostic -> diagnostic.severity == Severity.valueOf(name) }
                    } else {
                        Condition { diagnostic -> name == diagnostic.factory.name }
                    }

                when (operation) {
                    "!" -> {
                        if (!first) {
                            Assert.fail(
                                "'$operation$name' appears in a position rather than the first one, " +
                                        "which effectively cancels all the previous filters in this directive"
                            )
                        }
                        condition = newCondition
                    }
                    "+" -> condition = Conditions.or(condition, newCondition)
                    "-" -> condition = Conditions.and(condition, Conditions.not(newCondition))
                }
                first = false
            } while (matcher.find())

            // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
            return Conditions.or(
                condition,
                Condition { diagnostic -> diagnostic.factory in DIAGNOSTICS_TO_INCLUDE_ANYWAY }
            )
        }
    }

    private fun parseJvmTarget(directiveMap: Directives) = directiveMap[JVM_TARGET]?.let { JvmTarget.fromString(it) }
}
