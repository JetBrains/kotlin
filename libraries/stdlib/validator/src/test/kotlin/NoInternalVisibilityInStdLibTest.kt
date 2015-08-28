/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.fail

private val ANALYZE_PACKAGE_ROOTS_FOR_JVM = listOf("kotlin")
private val ANALYZE_PACKAGE_ROOTS_FOR_JS = listOf("kotlin", "jquery", "html5")

// these lists are not designed to contain all packages, they need for sanity check in case test code breaks
private val ADDITIONALLY_REQUIRED_PACKAGES_FOR_JVM = listOf("kotlin.jvm", "kotlin.concurrent")
private val ADDITIONALLY_REQUIRED_PACKAGES_FOR_JS = listOf("kotlin.js", "kotlin.reflect")

private val KOTLIN_ROOT_PATH = "../../../"

class NoInternalVisibilityInStdLibTest {
    private var disposable: Disposable? = null

    private class OutputSink(private val requiredPackages: List<FqName>) {
        private val internalDescriptors = ArrayList<DeclarationDescriptor>()
        private val validatedPackages = HashSet<FqName>()

        fun reportInternalVisibility(descriptor: DeclarationDescriptor) {
            internalDescriptors.add(descriptor)
        }

        fun reportValidatedPackage(packageFqName: FqName) {
            validatedPackages.add(packageFqName)
        }

        fun reportErrors() {
            println("Validated packages: ")
            validatedPackages.forEach { println("  $it") }

            if (!validatedPackages.containsAll(requiredPackages)) {
                fail("Some of the expected stdlib packages were not validated: " +
                        requiredPackages.subtract(validatedPackages))
            }

            if (internalDescriptors.isEmpty()) return

            val byFile = internalDescriptors.groupBy { descriptor ->
                DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)?.containingFile as JetFile
            }
            val byPackage = byFile.keySet().groupBy { it.packageFqName }

            val message = StringBuilder {
                appendln("There are ${internalDescriptors.size()} descriptors that have internal visibility:")
                for ((packageFqName, files) in byPackage) {

                    appendln("In package $packageFqName:")
                    for (file in files) {

                        appendln("In file ${file.name}")
                        appendln("--------------")
                        val descriptors = byFile[file]!!
                        descriptors.forEach {
                            descriptor ->
                            appendln("*    " + DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor))
                        }
                        appendln("--------------")
                    }
                    appendln()
                }
            }.toString()
            fail(message)
        }
    }

    @Before
    fun setUp() {
        disposable = Disposer.newDisposable()
    }

    @After
    fun tearDown() {
        Disposer.dispose(disposable!!)
        disposable = null
    }

    @Test
    fun testJvmStdlib() {
        doTest(ANALYZE_PACKAGE_ROOTS_FOR_JVM, ADDITIONALLY_REQUIRED_PACKAGES_FOR_JVM) {
            val configuration = CompilerConfiguration()
            configuration.addKotlinSourceRoot("../src/kotlin")
            configuration.addKotlinSourceRoot("../src/generated")
            configuration.addJvmClasspathRoots(PathUtil.getJdkClassesRoots())

            val environment = KotlinCoreEnvironment.createForProduction(disposable!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                    TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, "test"),
                    environment.getSourceFiles(), CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(), null, null,
                    JvmPackagePartProvider(environment)
            ).moduleDescriptor

        }
    }

    @Test
    fun testJsStdlibJar() {
        doTest(ANALYZE_PACKAGE_ROOTS_FOR_JS, ADDITIONALLY_REQUIRED_PACKAGES_FOR_JS) {
            val configuration = CompilerConfiguration()
            val environment = KotlinCoreEnvironment.createForProduction(disposable!!, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
            val project = environment.project
            val pathToJsStdlibJar = KOTLIN_ROOT_PATH + PathUtil.getKotlinPathsForDistDirectory().jsStdLibJarPath.path
            val config = LibrarySourcesConfig.Builder(project, "testModule", listOf(pathToJsStdlibJar))
                    .ecmaVersion(EcmaVersion.defaultVersion())
                    .sourceMap(false)
                    .inlineEnabled(false)
                    .build()

            TopDownAnalyzerFacadeForJS.analyzeFiles(listOf(), config).moduleDescriptor
        }
    }

    private fun doTest(
            testPackages: List<String>,
            additionallyRequiredPackages: List<String>,
            createTestModule: () -> ModuleDescriptor
    ) {
        val module = createTestModule()

        val requiredPackages = testPackages + additionallyRequiredPackages
        val sink = OutputSink(requiredPackages map { FqName(it) })

        for (testPackage in testPackages) {
            val packageView = module.getPackage(FqName(testPackage))
            validateDescriptor(module, packageView, sink)
        }

        sink.reportErrors()
    }

    private fun validateDescriptor(module: ModuleDescriptor, descriptor: DeclarationDescriptor, sink: OutputSink) {
        if (DescriptorUtils.getContainingModule(descriptor) != module) return

        if (descriptor is DeclarationDescriptorWithVisibility) {
            if (descriptor.visibility == Visibilities.INTERNAL) {
                val psi = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
                if (psi !is JetModifierListOwner || psi.modifierList?.hasModifier(JetTokens.INTERNAL_KEYWORD) != true) {
                    sink.reportInternalVisibility(descriptor)
                }
            }
        }
        when (descriptor) {
            is ClassDescriptor -> descriptor.defaultType.memberScope.getAllDescriptors().forEach {
                validateDescriptor(module, it, sink)
            }
            is PackageViewDescriptor -> {
                sink.reportValidatedPackage(DescriptorUtils.getFqName(descriptor).toSafe())
                descriptor.memberScope.getAllDescriptors().forEach {
                    validateDescriptor(module, it, sink)
                }
            }
        }
    }
}
