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

package org.jetbrains.kotlin

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.*

import java.nio.file.Paths
import java.util.function.Function
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.io.File

class RunExternalTestGroup extends JavaExec implements CompilerRunner {
    private def platformManager = project.project(":kotlin-native").platformManager
    private def target = platformManager.targetManager(project.testTarget).target

    @Internal
    def dist = UtilsKt.getKotlinNativeDist(project)

    @Input
    def enableKonanAssertions = true

    @Input
    def verifyIr = true

    @Input
    @Optional
    String outputDirectory = null
    @Input
    @Optional
    String goldValue = null

    @Internal
    // Checks test's output against gold value and returns true if the output matches the expectation
    Function<String, Boolean> outputChecker = { str -> (goldValue == null || goldValue == str) }

    @Input
    boolean printOutput = true

    @Input
    @Optional
    String testData = null

    @Input
    int expectedExitStatus = 0

    @Input
    @Optional
    List<String> arguments = null

    @Input
    @Optional
    List<String> flags = null

    @Input
    boolean multiRuns = false
    @Input
    @Optional
    List<List<String>> multiArguments = null

    @Input
    boolean expectedFail = false

    @Input
    boolean compilerMessages = false

    // Uses directory defined in $outputSourceSetName source set
    void createOutputDirectory() {
        if (outputDirectory != null) {
            return
        }
        def outputSourceSet = UtilsKt.getTestOutputExternal(project)
        outputDirectory = Paths.get(outputSourceSet, name).toString()
        project.file(outputDirectory).mkdirs()
    }

    RunExternalTestGroup() {
        // We don't build the compiler if a custom dist path is specified.
        UtilsKt.dependsOnDist(this)
        main = 'org.jetbrains.kotlin.cli.bc.K2NativeKt'
    }

    @Override
    void exec() {
        // Perhaps later we will return this exec() back but for now rest of infrastructure expects
        // compilation begins on runCompiler call, to emulate this behaviour we call super.exec() after
        // configuration part at runCompiler.
    }

    @Override
    void runCompiler(List<String> filesToCompile, String output, List<String> moreArgs) {
        def log = new ByteArrayOutputStream()
        try {
            classpath = project.fileTree("$dist.canonicalPath/konan/lib/") {
                include '*.jar'
            }
            jvmArgs "-Xmx4G"
            jvmArgs "-Dkonan.home=${UtilsKt.getKotlinNativeDist(project)}"
            enableAssertions = true
            def sources = File.createTempFile(name, ".lst")
            sources.deleteOnExit()
            def sourcesWriter = sources.newWriter()
            filesToCompile.each { f ->
                sourcesWriter.write(f.chars().any { Character.isWhitespace(it) }
                        ? "\"${f.replace("\\", "\\\\")}\"\n" // escape file name
                        : "$f\n")
            }
            sourcesWriter.close()
            args = ["-output", output,
                    "@${sources.absolutePath}",
                    *moreArgs,
                    *project.globalTestArgs]
            if (project.testTarget) {
                args "-target", target.visibleName
            }
            if (enableKonanAssertions) {
                args "-ea"
            }
            if (verifyIr) {
                args "-Xverify-ir"
            }
            if (project.hasProperty("test_verbose")) {
                println("Files to compile: $filesToCompile")
                println(args)
            }
            standardOutput = log
            errorOutput = log
            super.exec()
        } finally {
            def logString = log.toString("UTF-8")
            project.file("${output}.compilation.log").write(logString)
            println(logString)
        }
    }

    // FIXME: output directory here changes and hence this is not a property
    String executablePath() { return "$outputDirectory/program.tr" }

    @Internal
    OutputStream out

    void runExecutable() {
        if (!enabled) {
            println "Test is disabled: $name"
            return
        }
        def program = executablePath()
        def suffix = target.family.exeSuffix
        def exe = "$program.$suffix"

        println "execution: $exe"

        def compilerMessagesText = compilerMessages ? project.file("${program}.compilation.log").getText('UTF-8') : ""

        out = new ByteArrayOutputStream()
        //TODO Add test timeout

        def times = multiRuns ? multiArguments.size() : 1

        def exitCodeMismatch = false
        for (int i = 0; i < times; i++) {
            ExecResult execResult = project.extensions.executor.execute {

                commandLine exe

                if (arguments != null) {
                    args arguments
                }
                if (multiRuns && multiArguments[i] != null) {
                    args multiArguments[i]
                }
                if (testData != null) {
                    standardInput = new ByteArrayInputStream(testData.bytes)
                }
                standardOutput = out

                ignoreExitValue = true
            }

            exitCodeMismatch |= execResult.exitValue != expectedExitStatus
            if (exitCodeMismatch) {
                def message = "Expected exit status: $expectedExitStatus, actual: ${execResult.exitValue}"
                if (this.expectedFail) {
                    println("Expected failure. $message")
                } else {
                    throw new TestFailedException("Test failed on iteration $i. $message\n ${out.toString("UTF-8")}")
                }
            }
        }
        def result = compilerMessagesText + out.toString("UTF-8")
        if (printOutput) {
            println(result)
        }
        result = result.replace(System.lineSeparator(), "\n")
        def goldValueMismatch = !outputChecker.apply(result)
        if (goldValueMismatch) {
            def message
            if (goldValue != null) {
                message = "Expected output: $goldValue, actual output: $result"
            } else {
                message = "Actual output doesn't match output checker: $result"
            }
            if (this.expectedFail) {
                println("Expected failure. $message")
            } else {
                throw new TestFailedException("Test failed. $message")
            }
        }

        if (!exitCodeMismatch && !goldValueMismatch && this.expectedFail) println("Unexpected pass")
    }

    /**
     * If true, the test executable will be built in two stages:
     * 1. Build a klibrary from sources.
     * 2. Build a final executable from this klibrary.
     */
    @Input
    def enableTwoStageCompilation = false

    @Input
    def groupDirectory = "."

    @Input
    @Optional
    List<String> ignoredTests = null

    @Input
    @Optional
    String filter = project.findProperty("filter")

    @Internal
    def testGroupReporter = new KonanTestGroupReportEnvironment(project)

    void parseLanguageFlags(String src) {
        def text = project.rootProject.file(src).text
        def languageSettings = findLinesWithPrefixesRemoved(text, "// !LANGUAGE: ") +
                findLinesWithPrefixesRemoved(text, '// LANGUAGE: ')
        if (languageSettings.size() != 0) {
            languageSettings.forEach { line ->
                line.split(" ").toList().forEach {
                    if (it != "+NewInference") // It is on already by default, but passing it explicitly turns on a special "compatibility mode" in FE which is not desirable.
                        flags.add("-XXLanguage:$it")
                }
            }
        }

        def experimentalSettings = findLinesWithPrefixesRemoved(text, "// !OPT_IN: ")
        if (experimentalSettings.size() != 0) {
            experimentalSettings.forEach { line ->
                line.split(" ").toList().forEach { flags.add("-opt-in=$it") }
            }
        }
        def expectActualLinker = findLinesWithPrefixesRemoved(text, "// EXPECT_ACTUAL_LINKER")
        if (expectActualLinker.size() != 0) {
            flags.add("-Xexpect-actual-linker")
        }
    }

    static String markMutableObjects(String text) {
        def lines = text.readLines()
        def result = new ArrayList<String>(lines.size())
        lines.forEach { line ->
            // FIXME: find only those who has vars inside
            // Find object declarations and companion objects
            if (line.matches("\\s*(private|public|internal)?\\s*object [a-zA-Z_][a-zA-Z0-9_]*\\s*.*")
                    || line.matches("\\s*(private|public|internal)?\\s*companion object.*")) {
                result += "@kotlin.native.ThreadLocal"
            }
            result += line
        }
        return result.join(System.lineSeparator())
    }

    static String insertInTextAfter(String text, String insert, String after) {
        def begin = text.indexOf(after)
        if (begin != -1) {
            def end = text.indexOf("\n", begin)
            text = text.substring(0, end) + insert + text.substring(end)
        } else {
            text = insert + text
        }
        return text
    }

    List<TestFile> createTestFiles(String src, TestModule defaultModule) {
        def identifier = /[a-zA-Z_][a-zA-Z0-9_]/
        def fullQualified = /[a-zA-Z_][a-zA-Z0-9_.]/
        def importRegex = /(?m)^\s*import\s+/

        def packagePattern = ~/(?m)^\s*package\s+(${fullQualified}*)/
        def boxPattern = ~/(?m)fun\s+box\s*\(\s*\)/
        def classPattern = ~/.*(class|object|enum|interface)\s+(${identifier}*).*/

        def sourceName = "_" + normalize(project.rootProject.file(src).name)
        def packages = new LinkedHashSet<String>()
        def imports = []
        def classes = []
        def vars = new HashSet<String>()  // variables that has the same name as a package
        TestModule mainModule = defaultModule
        def testFiles = TestDirectivesKt.buildCompileList(
                project.rootProject.file(src).toPath(),
                "$outputDirectory/${project.rootProject.file(src).name}",
                mainModule
        )
        for (TestFile testFile : testFiles) {
            def text = testFile.text
            def filePath = testFile.path
            if (text.contains('COROUTINES_PACKAGE')) {
                text = text.replace('COROUTINES_PACKAGE', 'kotlin.coroutines')
            }
            def pkg
            if (text =~ packagePattern) {
                pkg = (text =~ packagePattern)[0][1]
                if (!pkg.startsWith("kotlin")) {
                    packages.add(pkg)
                    pkg = "$sourceName.$pkg"
                    text = text.replaceFirst(packagePattern, "package $pkg")
                }
            } else {
                pkg = sourceName
                text = insertInTextAfter(text, "\npackage $pkg\n", "@file:")
            }
            if (text =~ boxPattern) {
                imports.add("${pkg}.*")
                mainModule = testFile.module
            }

            // Find mutable objects that should be marked as ThreadLocal
            if (filePath != "$outputDirectory/${project.rootProject.file(src).name}/helpers.kt") {
                text = markMutableObjects(text)
            }
            testFile.text = text
        }
        for (TestFile testFile : testFiles) {
            def text = testFile.text
            // Find if there are any imports in the file
            def matcher = (text =~ ~/${importRegex}(${fullQualified}*)/)
            if (matcher) {
                // Prepend package name to found imports
                for (int i = 0; i < matcher.count; i++) {
                    String importStatement = matcher[i][1]
                    def subImport = importStatement.with {
                        int dotIdx = indexOf('.')
                        dotIdx > 0 ? substring(0, dotIdx) : it
                    }
                    if (packages.contains(subImport) || classes.contains(subImport)) {
                        // add only to those who import packages or import classes from the test files
                        text = text.replaceFirst(~/${importRegex}${Pattern.quote(importStatement)}/,
                                "import $sourceName.$importStatement")
                    } else if (text =~ classPattern) {
                        // special case for import from the local class
                        def clsMatcher = (text =~ classPattern)
                        for (int j = 0; j < clsMatcher.count; j++) {
                            def cl = (text =~ classPattern)[j][2]
                            classes.add(cl)
                            if (subImport == cl) {
                                text = text.replaceFirst(~/${importRegex}${Pattern.quote(importStatement)}/,
                                        "import $sourceName.$importStatement")
                            }
                        }
                    }
                }
            } else if (packages.empty) {
                // Add import statement after package
                def pkg = null
                if (text =~ packagePattern) {
                    pkg = 'package ' + (text =~ packagePattern)[0][1]
                    text = text.replaceFirst(packagePattern, '')
                }
                text = insertInTextAfter(text, (pkg ? "\n$pkg\n" : "") + "import $sourceName.*\n", "@file:")
            }
            // now replace all package usages in full qualified names
            def res = ""                      // filesToCompile
            text.eachLine { line ->
                packages.each { pkg ->
                    // line contains val or var declaration or function parameter declaration
                    if ((line =~ ~/va(l|r) *$pkg*( *: *$fullQualified*)?( *get\(\))? *\=.*/) ||
                            (line =~ ~/fun .*\(\n?\s*$pkg:.*/)) {
                        vars.add(pkg)
                    }
                    if (line.contains("$pkg.") && !(line =~ packagePattern || line =~ importRegex)
                            && !vars.contains(pkg)) {
                        def idx = 0
                        while ((idx = line.indexOf(pkg, idx)) >= 0) {
                            if (!Character.isJavaIdentifierPart(line.charAt(idx - 1))) {
                                line = line.substring(0, idx) + "$sourceName.$pkg" + line.substring(idx + pkg.length())
                                idx += sourceName.length() + pkg.length() + 1
                            } else {
                                idx += pkg.length()
                            }
                        }
                    }
                }
                res += "$line\n"
            }
            testFile.text = res
        }
        def launcherText = createLauncherFileText(src, imports)
        testFiles.add(
                new TestFile(
                        "_launcher.kt",
                        "$outputDirectory/$src/_launcher.kt".toString(),
                        launcherText,
                        mainModule
                )
        )
        return testFiles
    }

    String normalize(String name) {
        return name.replace('.kt', '')
                .replace('-', '_')
                .replace('.', '_')
    }

    /**
     * There are tests that require non-trivial 'package foo' in test launcher.
     */
    String createLauncherFileText(String src, List<String> imports) {
        StringBuilder text = new StringBuilder()
        def pack = normalize(project.file(src).name)
        text.append("package _$pack\n")
        for (v in imports) {
            text.append("import $v\n")
        }
        text.append(
"""
import kotlin.test.Test

@Test
fun runTest() {
    @Suppress("UNUSED_VARIABLE")
    val result = box()
    if (result != "OK") throw AssertionError("Test failed with: " + result)
}
"""     )
        return text.toString()
    }

    List<String> findLinesWithPrefixesRemoved(String text, String prefix) {
        def result = []
        text.eachLine {
            if (it.startsWith(prefix)) {
                result.add(it - prefix)
            }
        }
        return result
    }

    static def excludeList = [
            "compiler/testData/codegen/boxInline/multiplatform/defaultArguments/receiversAndParametersInLambda.kt", // KT-36880
            "compiler/testData/codegen/box/compileKotlinAgainstKotlin/specialBridgesInDependencies.kt",      // KT-42723
            "compiler/testData/codegen/box/collections/kt41123.kt",                                 // KT-42723
            "compiler/testData/codegen/box/multiplatform/multiModule/expectActualTypealiasLink.kt", // KT-40137
            "compiler/testData/codegen/box/multiplatform/multiModule/expectActualMemberLink.kt",    // KT-33091
            "compiler/testData/codegen/box/multiplatform/multiModule/expectActualLink.kt",          // KT-41901
            "compiler/testData/codegen/box/coroutines/multiModule/",                                // KT-40121
            "compiler/testData/codegen/box/compileKotlinAgainstKotlin/clashingFakeOverrideSignatures.kt"   // KT-42020
    ]

    boolean isEnabledForNativeBackend(String fileName) {
        def testFile = project.rootProject.file(fileName)
        def text = testFile.text

        if (testFile.name in ignoredTests)
            return false

        if (excludeList.any { fileName.replace(File.separator, "/").contains(it) }) return false

        def languageSettings = findLinesWithPrefixesRemoved(text, '// !LANGUAGE: ') +
                findLinesWithPrefixesRemoved(text, '// LANGUAGE: ')
        if (!languageSettings.empty) {
            def settings = languageSettings.first()
            if (settings.contains('-ProperIeee754Comparisons') ||  // K/N supports only proper IEEE754 comparisons
                    settings.contains('-ReleaseCoroutines') ||     // only release coroutines
                    settings.contains('-DataClassInheritance') ||  // old behavior is not supported
                    settings.contains('-ProhibitAssigningSingleElementsToVarargsInNamedForm') ||  // Prohibit these assignments
                    settings.contains('-ProhibitDataClassesOverridingCopy') ||  // Prohibit as no longer supported
                    settings.contains('-ProhibitOperatorMod') ||  // Prohibit as no longer supported
                    settings.contains('-UseBuilderInferenceOnlyIfNeeded') || // Run only default one
                    settings.contains('-UseCorrectExecutionOrderForVarargArguments')) {  // Run only correct one
                return false
            }
        }
        def diagnostics = findLinesWithPrefixesRemoved(text, '// !DIAGNOSTICS')
        if (!diagnostics.empty) {
            return false
        }

        def version = findLinesWithPrefixesRemoved(text, '// LANGUAGE_VERSION: ')
        if (version.size() != 0 && (!version.contains("1.3") || !version.contains("1.4"))) {
            // Support tests for 1.3 and exclude 1.2
            return false
        }

        def apiVersion = findLinesWithPrefixesRemoved(text, '// !API_VERSION: ')
        if (apiVersion.size() != 0 && !apiVersion.contains("1.4")) {
            return false
        }

        def targetBackend = findLinesWithPrefixesRemoved(text, "// TARGET_BACKEND")
        if (targetBackend.size() != 0) {
            // There is some target backend. Check if it is NATIVE or not.
            for (String s : targetBackend) {
                if (s.contains("NATIVE")){ return true }
            }
            return false
        } else {
            // No target backend. Check if NATIVE backend is ignored.
            def ignoredBackends = findLinesWithPrefixesRemoved(text, "// IGNORE_BACKEND: ") +
                    findLinesWithPrefixesRemoved(text, "// DONT_TARGET_EXACT_BACKEND: ")
            for (String s : ignoredBackends) {
                if (s.contains("NATIVE")) { return false }
            }
            // No ignored backends. Check if test is targeted to FULL_JDK or has JVM_TARGET set
            if (!findLinesWithPrefixesRemoved(text, "// FULL_JDK").isEmpty()) { return false }
            if (!findLinesWithPrefixesRemoved(text, "// JVM_TARGET:").isEmpty()) { return false }
            return true
        }
    }

    @TaskAction
    void executeTest() {
        createOutputDirectory()
        // Form the test list.
        def ktFiles = project.rootProject.projectDir.toPath().resolve(groupDirectory).toFile()
                .listFiles({
                    it.isFile() && it.name.endsWith(".kt")
                } as FileFilter)
        if (filter != null) {
            def pattern = ~filter
            ktFiles = ktFiles.findAll {
                it.name =~ pattern
            }
        }

        def defaultModule = TestModule.default()

        testGroupReporter.suite(name) { suite ->
            // Build tests in the group
            flags = (flags ?: []) + "-tr"
            List<TestFile> compileList = []
            ktFiles.each {
                def src = project.rootProject.relativePath(it)
                if (isEnabledForNativeBackend(src)) {
                    // Create separate output directory for each test in the group.
                    parseLanguageFlags(src)
                    compileList.addAll(createTestFiles(src, defaultModule))
                }
            }
            if (compileList.any { it.module.dependencies.contains("support") }) {
                def supportModule = TestModule.support()
                compileList.add(new TestFile("helpers.kt", "$outputDirectory/helpers.kt",
                        CoroutineTestUtilKt.createTextForHelpers(), supportModule))
            }
            compileList*.writeTextToFile()
            try {
                if (enableTwoStageCompilation) {
                    // Two-stage compilation.
                    def klibPath = "${executablePath()}.klib"
                    def files = compileList.stream()
                            .map { it.path }
                            .collect(Collectors.toList())
                    if (!files.empty) {
                        runCompiler(files, klibPath, flags + ["-p", "library"])
                        runCompiler([], executablePath(), flags + ["-Xinclude=$klibPath"])
                    }
                } else {
                    // Regular compilation with modules.
                    Map<String, TestModule> modules = compileList.stream()
                            .map {
                                println(it.module)
                                it.module }
                            .distinct()
                            .collect(Collectors.toMap({ it.name }, UnaryOperator.identity()))

                    List<TestModule> orderedModules = DFS.INSTANCE.topologicalOrder(modules.values()) { module ->
                        module.dependencies.collect { modules[it] }.findAll { it != null }
                    }
                    def compiler = new MultiModuleCompilerInvocations(this, outputDirectory, executablePath(), modules, flags)

                    orderedModules.reverse().each { module ->
                        println("${module.name}  ${module.dependencies}")
                        if (!module.isDefaultModule()) {
                            compiler.produceLibrary(module)
                        }
                    }

                    compiler.produceProgram(compileList)
                }
            } catch (Exception ex) {
                project.logger.quiet("ERROR: Compilation failed for test suite: $name with exception", ex)
                project.logger.quiet("The following files were unable to compile:")
                ktFiles.each { project.logger.quiet(it.name) }
                suite.abort("Compilation failed for test suite: $name", ex, ktFiles.collect { it.name })
                return
            }

            // Run the tests.
            arguments = (arguments ?: []) + "--ktest_logger=SILENT"
            ktFiles.each { file ->
                def src = project.rootProject.relativePath(file)
                def savedArgs = arguments
                arguments += "--ktest_filter=_${normalize(file.name)}.*"
                use(KonanTestSuiteReportKt) {
                    project.logger.quiet("TEST: $file.name " +
                            "(done: $testGroupReporter.statistics.total/${ktFiles.size()}, " +
                            "passed: $testGroupReporter.statistics.passed, " +
                            "skipped: $testGroupReporter.statistics.skipped)")
                }
                if (isEnabledForNativeBackend(src)) {
                    suite.executeTest(file.name) {
                        project.logger.quiet(src)
                        runExecutable()
                    }
                } else {
                    suite.skipTest(file.name)
                }
                arguments = savedArgs
            }
        }
    }
}
