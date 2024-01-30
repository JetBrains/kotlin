/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.DISABLE_NATIVE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.DISABLE_NATIVE_K1
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.DISABLE_NATIVE_K2
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_CINTEROP_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.IGNORE_NATIVE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.IGNORE_NATIVE_K1
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.IGNORE_NATIVE_K2
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.OUTPUT_DATA_FILE
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.InTextDirectivesUtils.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

internal class ExtTestCaseGroupProvider : TestCaseGroupProvider, TestDisposable(parentDisposable = null) {
    private val structureFactory = ExtTestDataFileStructureFactory(parentDisposable = this)
    private val sharedModules = ThreadSafeCache<String, TestModule.Shared?>()

    private val cachedTestCaseGroups = ThreadSafeCache<TestCaseGroupId.TestDataDir, TestCaseGroup?>()

    override fun getTestCaseGroup(testCaseGroupId: TestCaseGroupId, settings: Settings): TestCaseGroup? {
        assertNotDisposed()
        check(testCaseGroupId is TestCaseGroupId.TestDataDir)

        return cachedTestCaseGroups.computeIfAbsent(testCaseGroupId) {
            val testDataDir = testCaseGroupId.dir

            val excludes: Set<File> = settings.get<DisabledTestDataFiles>().filesAndDirectories
            if (testDataDir in excludes)
                return@computeIfAbsent TestCaseGroup.ALL_DISABLED

            val (excludedTestDataFiles, testDataFiles) = testDataDir.listFiles()
                ?.filter { file -> file.isFile && file.extension == "kt" }
                ?.partition { file -> file in excludes }
                ?: return@computeIfAbsent null

            val disabledTestCaseIds = hashSetOf<TestCaseId>()
            excludedTestDataFiles.mapTo(disabledTestCaseIds, TestCaseId::TestDataFile)

            val testCases = mutableListOf<TestCase>()

            testDataFiles.forEach { testDataFile ->
                val extTestDataFile = ExtTestDataFile(
                    testDataFile = testDataFile,
                    structureFactory = structureFactory,
                    customSourceTransformers = settings.get<ExternalSourceTransformersProvider>().getSourceTransformers(testDataFile),
                    settings = settings,
                )

                if (extTestDataFile.isRelevant)
                    testCases += extTestDataFile.createTestCase(
                        settings = settings,
                        sharedModules = sharedModules
                    )
                else
                    disabledTestCaseIds += TestCaseId.TestDataFile(testDataFile)
            }

            TestCaseGroup.Default(disabledTestCaseIds, testCases)
        }
    }
}

private class ExtTestDataFile(
    private val testDataFile: File,
    structureFactory: ExtTestDataFileStructureFactory,
    customSourceTransformers: ExternalSourceTransformers?,
    settings: Settings,
) {
    private val testRoots = settings.get<TestRoots>()
    private val generatedSources = settings.get<GeneratedSources>()
    private val customKlibs = settings.get<CustomKlibs>()
    private val timeouts = settings.get<Timeouts>()
    private val pipelineType = settings.get<PipelineType>()
    private val testMode = settings.get<TestMode>()
    private val cacheMode = settings.get<CacheMode>()
    private val optimizationMode = settings.get<OptimizationMode>()

    private val structure by lazy {
        val allSourceTransformers: ExternalSourceTransformers = if (customSourceTransformers.isNullOrEmpty())
            MANDATORY_SOURCE_TRANSFORMERS
        else
            MANDATORY_SOURCE_TRANSFORMERS + customSourceTransformers

        structureFactory.ExtTestDataFileStructure(testDataFile, allSourceTransformers).also {
            assertFalse(it.directives.contains(OUTPUT_DATA_FILE.name)) {
                "${testDataFile.absolutePath}: directive ${OUTPUT_DATA_FILE.name} is not supported by ExtTestDataFile"
            }
        }
    }

    private val isExpectedFailure: Boolean = settings.isIgnoredTarget(structure.directives)

    private val testDataFileSettings by lazy {
        val optIns = structure.directives.multiValues(OPT_IN_DIRECTIVE)
        val optInsForSourceCode = optIns subtract OPT_INS_PURELY_FOR_COMPILER
        val optInsForCompiler = optIns intersect OPT_INS_PURELY_FOR_COMPILER

        ExtTestDataFileSettings(
            languageSettings = structure.directives.multiValues(LANGUAGE_DIRECTIVE) {
                // It is already on by default, but passing it explicitly turns on a special "compatibility mode" in FE,
                // which is not desirable.
                it != "+NewInference"
            },
            optInsForSourceCode = optInsForSourceCode + structure.directives.multiValues(USE_EXPERIMENTAL_DIRECTIVE),
            optInsForCompiler = optInsForCompiler,
            generatedSourcesDir = computeGeneratedSourcesDir(
                testDataBaseDir = testRoots.baseDir,
                testDataFile = testDataFile,
                generatedSourcesBaseDir = generatedSources.testSourcesDir
            ),
            nominalPackageName = computePackageName(
                testDataBaseDir = testRoots.baseDir,
                testDataFile = testDataFile
            ),
            assertionsMode = AssertionsMode.fromString(structure.directives[ASSERTIONS_MODE.name])
        )
    }

    val isRelevant: Boolean =
        isCompatibleTarget(TargetBackend.NATIVE, testDataFile) // Checks TARGET_BACKEND/DONT_TARGET_EXACT_BACKEND directives.
                && !settings.isDisabledNative(structure.directives)
                && testDataFileSettings.languageSettings.none { it in INCOMPATIBLE_LANGUAGE_SETTINGS }
                && INCOMPATIBLE_DIRECTIVES.none { it in structure.directives }
                && structure.directives[API_VERSION_DIRECTIVE] !in INCOMPATIBLE_API_VERSIONS
                && structure.directives[LANGUAGE_VERSION_DIRECTIVE] !in INCOMPATIBLE_LANGUAGE_VERSIONS
                && !(FILECHECK_STAGE.name in structure.directives
                     && (cacheMode as? CacheMode.WithStaticCache)?.useStaticCacheForUserLibraries == true)
                && !(optimizationMode != OptimizationMode.OPT && structure.directives[FILECHECK_STAGE.name] == "OptimizeTLSDataLoads")
                && !(testDataFileSettings.languageSettings.contains("+${LanguageFeature.MultiPlatformProjects.name}")
                     && pipelineType == PipelineType.K2
                     && testMode == TestMode.ONE_STAGE_MULTI_MODULE)

    private fun assembleFreeCompilerArgs(): TestCompilerArgs {
        val args = mutableListOf<String>()
        structure.directives.listValues(FREE_COMPILER_ARGS.name)?.let { args.addAll(it)}
        testDataFileSettings.languageSettings.sorted().mapTo(args) { "-XXLanguage:$it" }
        testDataFileSettings.optInsForCompiler.sorted().mapTo(args) { "-opt-in=$it" }
        args += "-opt-in=kotlin.native.internal.InternalForKotlinNative" // for `Any.isPermanent()` and `Any.isLocal()`
        args += "-opt-in=kotlin.native.internal.InternalForKotlinNativeTests" // for ReflectionPackageName
        val freeCInteropArgs = structure.directives.listValues(FREE_CINTEROP_ARGS.name)
            .orEmpty().flatMap { it.split(" ") }
            .map { it.replace("\$generatedSourcesDir", testDataFileSettings.generatedSourcesDir.absolutePath) }
        return TestCompilerArgs(args, freeCInteropArgs, testDataFileSettings.assertionsMode)
    }

    fun createTestCase(settings: Settings, sharedModules: ThreadSafeCache<String, TestModule.Shared?>): TestCase {
        assertTrue(isRelevant)

        val definitelyStandaloneTest = settings.get<TestKind>() != TestKind.REGULAR
        val isStandaloneTest = definitelyStandaloneTest || determineIfStandaloneTest()
        patchPackageNames(isStandaloneTest)
        patchFileLevelAnnotations()
        val entryPointFunctionFQN = findEntryPoint()
        generateTestLauncher(isStandaloneTest, entryPointFunctionFQN)

        return doCreateTestCase(settings, isStandaloneTest, sharedModules)
    }

    /**
     * Determine if the current test should be compiled as a standalone test, i.e.
     * - package names are not patched
     * - test is compiled independently of any other tests
     */
    private fun determineIfStandaloneTest(): Boolean = with(structure) {
        if (directives.contains(NATIVE_STANDALONE_DIRECTIVE)) return true
        if (directives.contains(FILECHECK_STAGE.name)) return true
        if (directives.contains(ASSERTIONS_MODE.name)) return true
        if (isExpectedFailure) return true
        // To make the debug of possible failed testruns easier, it makes sense to run dodgy tests alone
        if (directives.contains(IGNORE_NATIVE.name) ||
            directives.contains(IGNORE_NATIVE_K1.name) ||
            directives.contains(IGNORE_NATIVE_K2.name)
        ) return true

        /**
         * K2 in MPP compilation expects that it receives module structure with exactly one platform leaf module
         * This invariant may be broken during grouping tests, so MPP tests should be run in standalone mode
         */
        if (pipelineType != PipelineType.K1 && testDataFileSettings.languageSettings.contains("+MultiPlatformProjects")) return true

        var isStandaloneTest = false

        filesToTransform.forEach { handler ->
            handler.accept(object : KtTreeVisitorVoid() {
                override fun visitKtFile(file: KtFile) = when {
                    isStandaloneTest -> Unit
                    file.packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME) -> {
                        // We can't fully patch packages for tests containing source code in any of kotlin.* packages.
                        // So, such tests should be run in standalone mode to avoid possible signature clashes with other tests.
                        isStandaloneTest = true
                    }
                    else -> super.visitKtFile(file)
                }
            })
        }

        isStandaloneTest
    }

    /**
     * For every Kotlin file (*.kt) stored in this text:
     *
     * - If there is a "package" declaration, patch it to prepend unique package prefix.
     *   Example: package foo -> package codegen.box.annotations.genericAnnotations.foo
     *
     * - If there is no "package" declaration, add one with the package name equal to unique package prefix.
     *   Example (new line added): package codegen.box.annotations.genericAnnotations
     *
     * - All "import" declarations are patched to reflect appropriate changes in "package" declarations.
     *   Example: import foo.* -> import codegen.box.annotations.genericAnnotations.foo.*
     *
     * - All fully-qualified references are patched to reflect appropriate changes in "package" declarations.
     *   Example: val x = foo.Bar() -> val x = codegen.box.annotations.genericAnnotations.foo.Bar()
     *
     * The "unique package prefix" is computed individually for every test file and reflects relative path to the test file.
     * Example: codegen/box/annotations/genericAnnotations.kt -> codegen.box.annotations.genericAnnotations
     *
     * Note that packages with fully-qualified name starting with "kotlin." and "helpers." are kept unchanged.
     * Examples: package kotlin.coroutines -> package kotlin.coroutines
     *           import kotlin.test.* -> import kotlin.test.*
     */
    private fun patchPackageNames(isStandaloneTest: Boolean) = with(structure) {
        if (isStandaloneTest) return // Don't patch packages for standalone tests.

        val basePackageName = FqName(testDataFileSettings.nominalPackageName.toString())

        val oldPackageNames: Set<FqName> = filesToTransform.mapToSet { it.packageFqName }
        val oldToNewPackageNameMapping: Map<FqName, FqName> = oldPackageNames.associateWith { oldPackageName ->
            basePackageName.child(oldPackageName)
        }

        filesToTransform.forEach { handler ->
            handler.accept(object : KtVisitor<Unit, Set<Name>>() {
                override fun visitKtElement(element: KtElement, parentAccessibleDeclarationNames: Set<Name>) {
                    element.getChildrenOfType<KtElement>().forEach { child ->
                        child.accept(this, parentAccessibleDeclarationNames)
                    }
                }

                override fun visitKtFile(file: KtFile, unused: Set<Name>) {
                    // Patch package directive.
                    val oldPackageDirective = file.packageDirective
                    val oldPackageName = oldPackageDirective?.fqName ?: FqName.ROOT

                    val newPackageName = oldToNewPackageNameMapping.getValue(file.packageFqNameForKLib)
                    val newPackageDirective = handler.psiFactory.createPackageDirective(newPackageName)

                    if (oldPackageDirective != null) {
                        // Replace old package directive by the new one.
                        oldPackageDirective.replace(newPackageDirective).ensureSurroundedByNewLines()
                    } else {
                        // Insert the package directive immediately after file-level annotations.
                        file.addAfter(newPackageDirective, file.fileAnnotationList).ensureSurroundedByNewLines()
                    }

                    if (!file.name.endsWith(".def")) { // don't process .def file contents after package directive
                        // Add @ReflectionPackageName annotation to make the compiler use original package name in the reflective information.
                        val annotationText =
                            "kotlin.native.internal.ReflectionPackageName(${oldPackageName.asString().quoteAsKotlinStringLiteral()})"
                        val fileAnnotationList = handler.psiFactory.createFileAnnotationListWithAnnotation(annotationText)
                        file.addAnnotations(fileAnnotationList)

                        visitKtElement(file, file.collectAccessibleDeclarationNames())
                    }
                }

                override fun visitPackageDirective(directive: KtPackageDirective, unused: Set<Name>) = Unit

                override fun visitImportDirective(importDirective: KtImportDirective, unused: Set<Name>) {
                    // Patch import directive if necessary.
                    val importedFqName = importDirective.importedFqName
                    if (importedFqName == null
                        || importedFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
                        || importedFqName.startsWith(KOTLINX_PACKAGE_NAME)
                        || importedFqName.startsWith(HELPERS_PACKAGE_NAME)
                        || importedFqName.startsWith(CNAMES_PACKAGE_NAME)
                        || importedFqName.startsWith(OBJCNAMES_PACKAGE_NAME)
                        || importedFqName.startsWith(PLATFORM_PACKAGE_NAME)
                    ) {
                        return
                    }

                    val newImportPath = ImportPath(
                        fqName = basePackageName.child(importedFqName),
                        isAllUnder = importDirective.isAllUnder,
                        alias = importDirective.aliasName?.let(Name::identifier)
                    )
                    importDirective.replace(handler.psiFactory.createImportDirective(newImportPath))
                }

                override fun visitTypeAlias(typeAlias: KtTypeAlias, parentAccessibleDeclarationNames: Set<Name>) =
                    super.visitTypeAlias(typeAlias, parentAccessibleDeclarationNames + typeAlias.collectAccessibleDeclarationNames())

                override fun visitClassOrObject(classOrObject: KtClassOrObject, parentAccessibleDeclarationNames: Set<Name>) =
                    super.visitClassOrObject(
                        classOrObject,
                        parentAccessibleDeclarationNames + classOrObject.collectAccessibleDeclarationNames()
                    )

                override fun visitClassBody(classBody: KtClassBody, parentAccessibleDeclarationNames: Set<Name>) =
                    super.visitClassBody(classBody, parentAccessibleDeclarationNames + classBody.collectAccessibleDeclarationNames())

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor, parentAccessibleDeclarationNames: Set<Name>) =
                    transformDeclarationWithBody(accessor, parentAccessibleDeclarationNames)

                override fun visitNamedFunction(function: KtNamedFunction, parentAccessibleDeclarationNames: Set<Name>) =
                    transformDeclarationWithBody(function, parentAccessibleDeclarationNames)

                override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, parentAccessibleDeclarationNames: Set<Name>) =
                    transformDeclarationWithBody(constructor, parentAccessibleDeclarationNames)

                override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, parentAccessibleDeclarationNames: Set<Name>) =
                    transformDeclarationWithBody(constructor, parentAccessibleDeclarationNames)

                private fun transformDeclarationWithBody(
                    declarationWithBody: KtDeclarationWithBody,
                    parentAccessibleDeclarationNames: Set<Name>
                ) {
                    val (expressions, nonExpressions) = declarationWithBody.getChildrenOfType<KtElement>().partition { it is KtExpression }

                    val accessibleDeclarationNames =
                        parentAccessibleDeclarationNames + declarationWithBody.collectAccessibleDeclarationNames()
                    nonExpressions.forEach { it.accept(this, accessibleDeclarationNames) }

                    val bodyAccessibleDeclarationNames =
                        accessibleDeclarationNames + declarationWithBody.valueParameters.map { it.nameAsSafeName }
                    expressions.forEach { it.accept(this, bodyAccessibleDeclarationNames) }
                }

                override fun visitExpression(expression: KtExpression, parentAccessibleDeclarationNames: Set<Name>) =
                    if (expression is KtFunctionLiteral)
                        transformDeclarationWithBody(expression, parentAccessibleDeclarationNames)
                    else
                        super.visitExpression(expression, parentAccessibleDeclarationNames)

                override fun visitBlockExpression(expression: KtBlockExpression, parentAccessibleDeclarationNames: Set<Name>) {
                    val accessibleDeclarationNames = parentAccessibleDeclarationNames.toMutableSet()
                    expression.getChildrenOfType<KtElement>().forEach { child ->
                        child.accept(this, accessibleDeclarationNames)
                        accessibleDeclarationNames.addIfNotNull(child.name?.let(Name::identifier))
                    }
                }

                override fun visitDotQualifiedExpression(
                    dotQualifiedExpression: KtDotQualifiedExpression,
                    accessibleDeclarationNames: Set<Name>
                ) {
                    val names = dotQualifiedExpression.collectNames()

                    val newDotQualifiedExpression =
                        visitPossiblyTypeReferenceWithFullyQualifiedName(names, accessibleDeclarationNames) { newPackageName ->
                            val newDotQualifiedExpression = handler.psiFactory
                                .createFile("val x = ${newPackageName.asString()}.${dotQualifiedExpression.text}")
                                .getChildOfType<KtProperty>()!!
                                .getChildOfType<KtDotQualifiedExpression>()!!

                            dotQualifiedExpression.replace(newDotQualifiedExpression) as KtDotQualifiedExpression
                        } ?: dotQualifiedExpression

                    super.visitDotQualifiedExpression(newDotQualifiedExpression, accessibleDeclarationNames)
                }

                override fun visitUserType(userType: KtUserType, accessibleDeclarationNames: Set<Name>) {
                    val names = userType.collectNames()

                    val newUserType =
                        visitPossiblyTypeReferenceWithFullyQualifiedName(names, accessibleDeclarationNames) { newPackageName ->
                            val newUserType = handler.psiFactory
                                .createFile("val x: ${newPackageName.asString()}.${userType.text}")
                                .getChildOfType<KtProperty>()!!
                                .getChildOfType<KtTypeReference>()!!
                                .typeElement as KtUserType

                            userType.replace(newUserType) as KtUserType
                        } ?: userType

                    newUserType.typeArgumentList?.let { visitKtElement(it, accessibleDeclarationNames) }
                }

                private fun <T : KtElement> visitPossiblyTypeReferenceWithFullyQualifiedName(
                    names: List<Name>,
                    accessibleDeclarationNames: Set<Name>,
                    action: (newSubPackageName: FqName) -> T
                ): T? {
                    if (names.size < 2) return null

                    if (names.first() in accessibleDeclarationNames) return null

                    for (index in 1 until names.size) {
                        val subPackageName = names.fqNameBeforeIndex(index)
                        val newPackageName = oldToNewPackageNameMapping[subPackageName]
                        if (newPackageName != null)
                            return action(newPackageName.removeSuffix(subPackageName))
                    }

                    return null
                }
            }, emptySet())
        }
    }

    /**
     * Make sure that the OptIns specified in test directives (see [ExtTestDataFileSettings.optInsForSourceCode]) are represented
     * as file-level annotations in every individual test file.
     */
    private fun patchFileLevelAnnotations() = with(structure) {
        fun getAnnotationText(fullyQualifiedName: String) = "@file:${OPT_IN_ANNOTATION_NAME.asString()}($fullyQualifiedName::class)"

        // Make sure that every test file contains all the necessary file-level annotations.
        if (testDataFileSettings.optInsForSourceCode.isNotEmpty()) {
            filesToTransform.forEach { handler ->
                handler.accept(object : KtTreeVisitorVoid() {
                    override fun visitKtFile(file: KtFile) {
                        val newFileAnnotationList = handler.psiFactory.createFile(buildString {
                            testDataFileSettings.optInsForSourceCode.forEach {
                                appendLine(getAnnotationText(it))
                            }
                        }).fileAnnotationList!!

                        file.addAnnotations(newFileAnnotationList)
                    }
                })
            }
        }
    }

    private fun KtFile.addAnnotations(fileAnnotationList: KtFileAnnotationList) {
        val oldFileAnnotationList = this.fileAnnotationList
        if (oldFileAnnotationList != null) {
            // Add new annotations to the old ones.
            fileAnnotationList.annotationEntries.forEach {
                oldFileAnnotationList.add(it).ensureSurroundedByNewLines()
            }
        } else {
            // Insert the annotations list immediately before package directive.
            this.addBefore(fileAnnotationList, packageDirective).ensureSurroundedByNewLines()
        }
    }

    /** Finds the fully-qualified name of the entry point function (aka `fun box(): String`). */
    private fun findEntryPoint(): String = with(structure) {
        val result = mutableListOf<String>()

        filesToTransform.forEach { handler ->
            handler.accept(object : KtTreeVisitorVoid() {
                override fun visitKtFile(file: KtFile) {
                    val hasBoxFunction = file.getChildrenOfType<KtNamedFunction>().any { function ->
                        function.name == BOX_FUNCTION_NAME.asString() && function.valueParameters.isEmpty()
                    }

                    if (hasBoxFunction) {
                        val boxFunctionFqName = file.packageFqName.child(BOX_FUNCTION_NAME).asString()
                        result += boxFunctionFqName

                        handler.module.markAsMain()
                    }
                }
            })
        }

        return result.singleOrNull()
            ?: fail {
                "Exactly one entry point function is expected in $testDataFile. " +
                        "But ${if (result.size == 0) "none" else result.size} were found: $result"
            }
    }

    /** Adds a wrapper to run it as Kotlin test. */
    private fun generateTestLauncher(isStandaloneTest: Boolean, entryPointFunctionFQN: String) {
        val fileText = buildString {
            if (!isStandaloneTest) {
                append("package ").appendLine(testDataFileSettings.nominalPackageName)
                appendLine()
            }

            append(generateBoxFunctionLauncher(entryPointFunctionFQN))
        }

        structure.addFileToMainModule(fileName = LAUNCHER_FILE_NAME, text = fileText)
    }

    private fun doCreateTestCase(
        settings: Settings,
        isStandaloneTest: Boolean,
        sharedModules: ThreadSafeCache<String, TestModule.Shared?>
    ): TestCase = with(structure) {
        val modules = generateModules(
            testCaseDir = testDataFileSettings.generatedSourcesDir,
            findOrGenerateSharedModule = { moduleName: String, generator: SharedModuleGenerator ->
                sharedModules.computeIfAbsent(moduleName) {
                    generator(generatedSources.sharedSourcesDir)
                }
            }
        )
        val fileCheckStage = retrieveFileCheckStage()
        val testCase = TestCase(
            id = TestCaseId.TestDataFile(testDataFile),
            kind = if (isStandaloneTest) TestKind.STANDALONE else TestKind.REGULAR,
            modules = modules,
            freeCompilerArgs = assembleFreeCompilerArgs(),
            nominalPackageName = testDataFileSettings.nominalPackageName,
            expectedFailure = isExpectedFailure,
            checks = TestRunChecks.Default(timeouts.executionTimeout).copy(
                fileCheckMatcher = fileCheckStage?.let { TestRunCheck.FileCheckMatcher(settings, testDataFile) },
                // for expected failures, it does not matter, which exit code would the process have, since test might fail with other reasons
                exitCodeCheck = TestRunCheck.ExitCode.Expected(0).takeUnless { isExpectedFailure },
            ),
            fileCheckStage = fileCheckStage,
            extras = WithTestRunnerExtras(runnerType = TestRunnerType.DEFAULT)
        )
        testCase.initialize(
            givenModules = customKlibs.klibs.mapToSet(TestModule::Given),
            findSharedModule = sharedModules::get
        )

        return testCase
    }

    private fun retrieveFileCheckStage(): String? {
        val fileCheckStages = structure.directives.multiValues(FILECHECK_STAGE.name)
        return when (fileCheckStages.size) {
            0 -> {
                require(!isDirectiveDefined(testDataFile.readText(), FILECHECK_STAGE.name)) {
                    "In ${testDataFile.absolutePath}, one argument for FILECHECK directive is needed: LLVM stage name, to dump bitcode after"
                }
                null
            }
            1 -> fileCheckStages.single()
            else -> fail { "In ${testDataFile.absolutePath}, only one argument for FILECHECK directive is allowed: $fileCheckStages" }
        }
    }

    companion object {
        private val INCOMPATIBLE_DIRECTIVES = setOf("FULL_JDK", "JVM_TARGET", "DIAGNOSTICS")

        private const val API_VERSION_DIRECTIVE = "API_VERSION"
        private val INCOMPATIBLE_API_VERSIONS = setOf("1.4")

        private const val LANGUAGE_VERSION_DIRECTIVE = "LANGUAGE_VERSION"
        private val INCOMPATIBLE_LANGUAGE_VERSIONS = setOf("1.3", "1.4")

        private const val LANGUAGE_DIRECTIVE = "LANGUAGE"
        private val INCOMPATIBLE_LANGUAGE_SETTINGS = setOf(
            "-ProperIeee754Comparisons",                            // K/N supports only proper IEEE754 comparisons
            "-ReleaseCoroutines",                                   // only release coroutines
            "-DataClassInheritance",                                // old behavior is not supported
            "-ProhibitAssigningSingleElementsToVarargsInNamedForm", // Prohibit these assignments
            "-ProhibitDataClassesOverridingCopy",                   // Prohibit as no longer supported
            "-ProhibitOperatorMod",                                 // Prohibit as no longer supported
            "-ProhibitIllegalValueParameterUsageInDefaultArguments",  // Allow only legal values
            "-UseBuilderInferenceOnlyIfNeeded",                     // Run only default one
            "-UseCorrectExecutionOrderForVarargArguments"           // Run only correct one
        )

        private const val USE_EXPERIMENTAL_DIRECTIVE = "USE_EXPERIMENTAL"

        private const val NATIVE_STANDALONE_DIRECTIVE = "NATIVE_STANDALONE"

        private const val OPT_IN_DIRECTIVE = "OPT_IN"
        private val OPT_INS_PURELY_FOR_COMPILER = setOf(
            OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString()
        )

        private fun Directives.multiValues(key: String, predicate: (String) -> Boolean = { true }): Set<String> =
            listValues(key)?.flatMap { it.split(' ') }?.filter(predicate)?.toSet().orEmpty()

        private val BOX_FUNCTION_NAME = Name.identifier("box")
        private val OPT_IN_ANNOTATION_NAME = Name.identifier("OptIn")
        private val HELPERS_PACKAGE_NAME = Name.identifier("helpers")
        private val KOTLINX_PACKAGE_NAME = Name.identifier("kotlinx")
        private val CNAMES_PACKAGE_NAME = Name.identifier("cnames")
        private val OBJCNAMES_PACKAGE_NAME = Name.identifier("objcnames")
        private val PLATFORM_PACKAGE_NAME = Name.identifier("platform")

        private val MANDATORY_SOURCE_TRANSFORMERS: ExternalSourceTransformers = listOf(DiagnosticsRemovingSourceTransformer)
    }
}

private class ExtTestDataFileSettings(
    val languageSettings: Set<String>,
    val optInsForSourceCode: Set<String>,
    val optInsForCompiler: Set<String>,
    val generatedSourcesDir: File,
    val nominalPackageName: PackageName,
    val assertionsMode: AssertionsMode,
)

private typealias SharedModuleGenerator = (sharedModulesDir: File) -> TestModule.Shared?
private typealias SharedModuleCache = (moduleName: String, generator: SharedModuleGenerator) -> TestModule.Shared?

private class ExtTestDataFileStructureFactory(parentDisposable: Disposable) : TestDisposable(parentDisposable) {
    private val psiFactory = createPsiFactory(parentDisposable = this)

    inner class ExtTestDataFileStructure(originalTestDataFile: File, sourceTransformers: ExternalSourceTransformers) {
        init {
            assertNotDisposed()
        }

        private val filesAndModules = FilesAndModules(originalTestDataFile, sourceTransformers)

        val directives: Directives get() = filesAndModules.directives

        val filesToTransform: Iterable<CurrentFileHandler>
            get() = filesAndModules.parsedFiles.filter { it.key.name.endsWith(".kt") || it.key.name.endsWith(".def") }
                .map { (extTestFile, psiFile) ->
                    object : CurrentFileHandler {
                        override val packageFqName get() = psiFile.packageFqNameForKLib
                        override val module = object : CurrentFileHandler.ModuleHandler {
                            override fun markAsMain() {
                                extTestFile.module.isMain = true
                            }
                        }
                        override val psiFactory get() = this@ExtTestDataFileStructureFactory.psiFactory

                        override fun accept(visitor: KtVisitor<*, *>): Unit = psiFile.accept(visitor)
                        override fun <D> accept(visitor: KtVisitor<*, D>, data: D) {
                            psiFile.accept(visitor, data)
                        }
                    }
                }

        fun addFileToMainModule(fileName: String, text: String): Unit = filesAndModules.addFileToMainModule(fileName, text)

        fun generateModules(testCaseDir: File, findOrGenerateSharedModule: SharedModuleCache): Set<TestModule.Exclusive> {
            checkModulesConsistency()

            // Generate support module, if any.
            val supportModule = generateSharedSupportModule(findOrGenerateSharedModule)

            // Update texts of parsed test files.
            filesAndModules.parsedFiles.forEach { (extTestFile, psiFile) -> extTestFile.text = psiFile.text }

            // Transform internal model into Kotlin/Native test infrastructure test model.
            fun transformDependency(extTestModule: KotlinBaseTest.TestModule): String =
                if (extTestModule is ExtTestModule && extTestModule.isSupport && supportModule != null) {
                    // Is support module is met across dependencies, then return new (unique) name for it.
                    supportModule.name
                } else
                    extTestModule.name

            return filesAndModules.modules.values.mapNotNullToSet { extTestModule ->
                if (extTestModule.isSupport) return@mapNotNullToSet null

                serializeModuleToFileSystem(
                    source = extTestModule,
                    destination = TestModule.Exclusive(
                        name = extTestModule.name,
                        directDependencySymbols = extTestModule.dependencies.mapToSet(::transformDependency),
                        directFriendSymbols = extTestModule.friends.mapToSet(::transformDependency),
                        directDependsOnSymbols = extTestModule.dependsOn.mapToSet(::transformDependency),
                    ),
                    baseDir = testCaseDir
                ) { module, file -> module.files += file }
            }
        }

        private fun generateSharedSupportModule(findOrGenerateSharedModule: SharedModuleCache): TestModule.Shared? {
            val extTestSupportModule = filesAndModules.modules[SUPPORT_MODULE_NAME] ?: return null

            // Compute the module's hash. It will be used to give a unique name for the module.
            val prettyHash = prettyHash(
                extTestSupportModule.files.sortedBy { it.name }.fold(0) { hash, extTestFile ->
                    (hash * 31 + extTestFile.name.hashCode()) * 31 + extTestFile.text.hashCode()
                })
            val newModuleName = "${SUPPORT_MODULE_NAME}_$prettyHash"

            return findOrGenerateSharedModule(newModuleName) { sharedModulesDir ->
                serializeModuleToFileSystem(
                    source = extTestSupportModule,
                    destination = TestModule.Shared(newModuleName),
                    baseDir = sharedModulesDir
                ) { module, file -> module.files += file }
            }
        }

        private fun <T : TestModule> serializeModuleToFileSystem(
            source: ExtTestModule,
            destination: T,
            baseDir: File,
            process: (T, TestFile<T>) -> Unit
        ): T {
            val moduleDir = baseDir.resolve(destination.name)
            moduleDir.mkdirs()

            source.files.forEach { extTestFile ->
                val file = moduleDir.resolve(extTestFile.name)
                file.parentFile.mkdirs()
                file.writeText(extTestFile.text)
                process(destination, TestFile.createCommitted(file, destination))
            }

            return destination
        }

        private fun checkModulesConsistency() {
            filesAndModules.modules.values.forEach { module ->
                val unknownFriends = (module.friendsSymbols + module.friends.map { it.name }).toSet() - filesAndModules.modules.keys

                val unknownDependencies =
                    (module.dependenciesSymbols + module.dependencies.map { it.name }).toSet() - filesAndModules.modules.keys

                val unknownDependsOn =
                    (module.dependsOnSymbols + module.dependsOn.map { it.name }).toSet() - filesAndModules.modules.keys

                val unknownAllDependencies = unknownDependencies + unknownFriends + unknownDependsOn
                assertTrue(unknownAllDependencies.isEmpty()) { "Module $module has unknown dependencies: $unknownAllDependencies" }

                assertTrue(module.files.isNotEmpty()) { "Module $module has no files" }
            }
        }
    }

    private class ExtTestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>,
        dependsOn: List<String>, // mimics the name from ModuleStructureExtractorImpl, thought later converted to `-Xfragment-refines` parameter
    ) : KotlinBaseTest.TestModule(name, dependencies, friends, dependsOn) {
        val files = mutableListOf<ExtTestFile>()

        val isSupport get() = name == SUPPORT_MODULE_NAME
        var isMain = false

        override fun equals(other: Any?) = (other as? ExtTestModule)?.name == name
        override fun hashCode() = name.hashCode()
    }

    private class ExtTestFile(
        val name: String,
        val module: ExtTestModule,
        var text: String
    ) {
        init {
            module.files += this
        }

        override fun equals(other: Any?): Boolean {
            if (other !is ExtTestFile) return false
            return other.name == name && other.module == module
        }

        override fun hashCode() = name.hashCode() * 31 + module.hashCode()
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    private class ExtTestFileFactory : TestFiles.TestFileFactory<ExtTestModule, ExtTestFile> {
        private val defaultModule by lazy { createModule(DEFAULT_MODULE_NAME, emptyList(), emptyList(), emptyList()) }
        private val supportModule by lazy { createModule(SUPPORT_MODULE_NAME, emptyList(), emptyList(), emptyList()) }

        lateinit var directives: Directives

        fun createFile(module: ExtTestModule, fileName: String, text: String): ExtTestFile =
            ExtTestFile(getSanitizedFileName(fileName), module, text)

        override fun createFile(module: ExtTestModule?, fileName: String, text: String, directives: Directives): ExtTestFile {
            this.directives = directives
            return createFile(
                module = module ?: if (fileName == "CoroutineUtil.kt") supportModule else defaultModule,
                fileName = fileName,
                text = text
            )
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>, dependsOn: List<String>): ExtTestModule =
            ExtTestModule(name, dependencies, friends, dependsOn)
    }

    private inner class FilesAndModules(originalTestDataFile: File, sourceTransformers: ExternalSourceTransformers) {
        private val testFileFactory = ExtTestFileFactory()

        @OptIn(ObsoleteTestInfrastructure::class)
        private val generatedFiles = TestFiles.createTestFiles(
            /* testFileName = */ DEFAULT_FILE_NAME,
            /* expectedText = */ originalTestDataFile.readText(),
            /* factory = */ testFileFactory,
            /* preserveLocations = */ true
        )

        private val lazyData: Triple<Map<String, ExtTestModule>, Map<ExtTestFile, KtFile>, MutableList<ExtTestFile>> by lazy {
            // Clean up contents of every individual test file. Important: This should be done only after parsing testData file,
            // because parsing of testData file relies on certain directives which could be removed by the transformation.
            generatedFiles.forEach { file ->
                file.text = sourceTransformers.fold(file.text) { source, transformer -> transformer(source) }
            }

            val modules = generatedFiles.map { it.module }.associateBy { it.name }

            val (supportModuleFiles, nonSupportModuleFiles) = generatedFiles.partition { it.module.isSupport }
            val parsedFiles = nonSupportModuleFiles.associateWith { psiFactory.createFile(it.name, it.text) }
            val nonParsedFiles = supportModuleFiles.toMutableList()

            // Explicitly add support module to other modules' dependencies (as it is not listed there by default).
            val supportModule = modules[SUPPORT_MODULE_NAME]
            if (supportModule != null) {
                modules.forEach { (moduleName, module) ->
                    if (moduleName != SUPPORT_MODULE_NAME && supportModule !in module.dependencies) {
                        module.dependencies += supportModule
                    }
                }
            }

            Triple(modules, parsedFiles, nonParsedFiles)
        }

        val directives: Directives get() = testFileFactory.directives

        val modules: Map<String, ExtTestModule> get() = lazyData.first
        val parsedFiles: Map<ExtTestFile, KtFile> get() = lazyData.second
        private val nonParsedFiles: MutableList<ExtTestFile> get() = lazyData.third

        fun addFileToMainModule(fileName: String, text: String) {
            val foundModules = modules.values.filter { it.isMain }
            val mainModule = when (val size = foundModules.size) {
                1 -> foundModules.first()
                else -> fail { "Exactly one main module is expected. But ${if (size == 0) "none" else size} were found." }
            }

            nonParsedFiles += testFileFactory.createFile(mainModule, fileName, text)
        }
    }

    interface CurrentFileHandler {
        interface ModuleHandler {
            fun markAsMain()
        }

        val packageFqName: FqName
        val module: ModuleHandler
        val psiFactory: KtPsiFactory

        fun accept(visitor: KtVisitor<*, *>)
        fun <D> accept(visitor: KtVisitor<*, D>, data: D)
    }

    companion object {
        private val lock = Object()

        private fun createPsiFactory(parentDisposable: Disposable): KtPsiFactory {
            val configuration: CompilerConfiguration = KotlinTestUtils.newConfiguration()
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "native-blackbox-test-patching-module")

            val environment = KotlinCoreEnvironment.createForProduction(
                projectDisposable = parentDisposable,
                configuration = configuration,
                configFiles = EnvironmentConfigFiles.METADATA_CONFIG_FILES
            )

            synchronized(lock) {
                CoreApplicationEnvironment.registerApplicationDynamicExtensionPoint(
                    TreeCopyHandler.EP_NAME.name,
                    TreeCopyHandler::class.java
                )
            }

            val project = environment.project as MockProject
            project.registerService(PomModel::class.java, PomModelImpl::class.java)
            project.registerService(TreeAspect::class.java)

            return KtPsiFactory(environment.project)
        }
    }
}

internal fun Settings.isIgnoredTarget(testDataFile: File): Boolean {
    val disposable = Disposer.newDisposable("Disposable for ExtTestCaseGroupProvider.isIgnoredTarget")
    try {
        val extTestDataFileStructure = ExtTestDataFileStructureFactory(disposable).ExtTestDataFileStructure(testDataFile, emptyList())
        return isIgnoredTarget(extTestDataFileStructure.directives)
    } finally {
        Disposer.dispose(disposable)
    }
}

internal fun Settings.isIgnoredTarget(directives: Directives): Boolean {
    return isIgnoredWithIGNORE_BACKEND(directives) ||
            isIgnoredWithIGNORE_NATIVE(directives)
}

// Mimics `InTextDirectivesUtils.isIgnoredTarget(NATIVE, file)` but does not require file contents, but only already parsed directives.
private fun Settings.isIgnoredWithIGNORE_BACKEND(directives: Directives): Boolean {
    val containsNativeOrAny: (List<String>) -> Boolean = { TargetBackend.NATIVE.name in it || TargetBackend.ANY.name in it }

    if (directives.listValues(IGNORE_BACKEND.name)?.let(containsNativeOrAny) == true)
        return true
    when (get<PipelineType>()) {
        PipelineType.K1 ->
            if (directives.listValues(IGNORE_BACKEND_K1.name)?.let(containsNativeOrAny) == true)
                return true
        PipelineType.K2 ->
            if (directives.listValues(IGNORE_BACKEND_K2.name)?.let(containsNativeOrAny) == true)
                return true
        else -> {}
    }
    return false
}

private val TARGET_FAMILY = "targetFamily"
private val TARGET_ARCHITECTURE = "targetArchitecture"
private val IS_APPLE_TARGET = "isAppleTarget"
private val CACHE_MODE_NAMES = CacheMode.Alias.entries.map { it.name }
private val TEST_MODE_NAMES = TestMode.entries.map { it.name }
private val OPTIMIZATION_MODE_NAMES = OptimizationMode.entries.map { it.name }
private val GC_TYPE_NAMES = GCType.entries.map { it.name }
private val FAMILY_NAMES = Family.entries.map { it.name }
private val ARCHITECTURE_NAMES = Architecture.entries.map { it.name }
private val BOOLEAN_NAMES = listOf(true.toString(), false.toString())

private fun Settings.isDisabledNative(directives: Directives) =
    evaluate(getDirectiveValues(directives, DISABLE_NATIVE, DISABLE_NATIVE_K1, DISABLE_NATIVE_K2))

private fun Settings.isIgnoredWithIGNORE_NATIVE(directives: Directives) =
    evaluate(getDirectiveValues(directives, IGNORE_NATIVE, IGNORE_NATIVE_K1, IGNORE_NATIVE_K2))

// Evaluation of conjunction of boolean expressions like `property1=value1 && property2=value2`.
// Any null element makes whole result as `true`.
private fun Settings.evaluate(directiveValues: List<String?>): Boolean {
    directiveValues.forEach {
        if (it == null)
            return true  // Directive without value is treated as unconditional
        val split = it.split("&&")
        val booleanList = split.map {
            val matchResult = "(.+)=(.+)".toRegex().find(it.trim())
                ?: throw AssertionError("Invalid format for IGNORE_NATIVE* directive ($it). Must be <property>=<value>")
            val propName = matchResult.groups[1]?.value
            val (actualValue, supportedValues) = when (propName) {
                ClassLevelProperty.CACHE_MODE.shortName -> get<CacheMode>().alias.name to CACHE_MODE_NAMES
                ClassLevelProperty.TEST_MODE.shortName -> get<TestMode>().name to TEST_MODE_NAMES
                ClassLevelProperty.OPTIMIZATION_MODE.shortName -> get<OptimizationMode>().name to OPTIMIZATION_MODE_NAMES
                ClassLevelProperty.TEST_TARGET.shortName -> get<KotlinNativeTargets>().testTarget.name to null
                ClassLevelProperty.GC_TYPE.shortName -> get<GCType>().name to GC_TYPE_NAMES
                TARGET_FAMILY -> get<KotlinNativeTargets>().testTarget.family.name to FAMILY_NAMES
                TARGET_ARCHITECTURE -> get<KotlinNativeTargets>().testTarget.architecture.name to ARCHITECTURE_NAMES
                IS_APPLE_TARGET -> get<KotlinNativeTargets>().testTarget.family.isAppleFamily.toString() to BOOLEAN_NAMES
                else -> throw AssertionError("ClassLevelProperty name: $propName is not yet supported in IGNORE_NATIVE* test directives.")
            }
            val valueFromTestDirective = matchResult.groups[2]?.value!!
            supportedValues?.let {
                if (actualValue !in it)
                    throw AssertionError("Internal error: Test run value $propName=$actualValue is not in expected supported values: $it")
                if (valueFromTestDirective !in it)
                    throw AssertionError("Test directive `IGNORE_NATIVE*: $propName=$valueFromTestDirective` has unsupported value. Supported are: $it")
            }
            actualValue == valueFromTestDirective
        }
        val matches = booleanList.reduce { a, b -> a && b }
        if (matches)
            return true
    }
    return false
}

// Returns list of relevant directive values.
// Null is added to result list in case the directive given without value.
private fun Settings.getDirectiveValues(
    directives: Directives,
    directiveAllPipelineTypes: StringDirective,
    directiveK1: StringDirective,
    directiveK2: StringDirective
): List<String?> = buildList {
    fun extract(directives: Directives, directive: StringDirective) {
        if (directives.contains(directive.name))
            directives.listValues(directive.name)?.let { addAll(it) } ?: add(null)
    }
    extract(directives, directiveAllPipelineTypes)
    when (get<PipelineType>()) {
        PipelineType.K1 -> extract(directives, directiveK1)
        PipelineType.K2 -> extract(directives, directiveK2)
        else -> {}
    }
}

private val KtFile.packageFqNameForKLib: FqName
    get() = when (name.substringAfterLast(".")) {
        "kt" -> packageFqName
        "def" -> {
            // Without package directive, CInterop tool puts declarations to a package with kinda odd name, as such:
            // name of .def file without extension, splitted by dot-separated parts, and reversed.
            if (packageFqName != FqName.ROOT) packageFqName
            else FqName.fromSegments(name.removeSuffix(".def").split(".").reversed())
        }
        else -> TODO("File extension is not yet supported: $name")
    }
