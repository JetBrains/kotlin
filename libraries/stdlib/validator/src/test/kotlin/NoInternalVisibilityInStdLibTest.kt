/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.junit.Test
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import com.intellij.openapi.util.Disposer
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.renderer.DescriptorRenderer
import kotlin.test.fail
import org.jetbrains.jet.utils.PathUtil
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import java.util.HashSet
import org.jetbrains.jet.config.CommonConfigurationKeys
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.k2js.config.LibrarySourcesConfig
import org.jetbrains.k2js.config.EcmaVersion
import org.jetbrains.k2js.analyze.TopDownAnalyzerFacadeForJS
import com.intellij.openapi.Disposable

private val ANALYZE_PACKAGE_ROOTS_FOR_JVM = listOf("kotlin")
private val ANALYZE_PACKAGE_ROOTS_FOR_JS = listOf("kotlin", "jquery", "html5")

// these lists are not designed to contain all packages, they need for sanity check in case test code breaks
private val ADDITIONALLY_REQUIRED_PACKAGES_FOR_JVM = listOf("kotlin.jvm", "kotlin.concurrent")
private val ADDITIONALLY_REQUIRED_PACKAGES_FOR_JS = listOf("kotlin.js", "kotlin.reflect")

private val KOTLIN_ROOT_PATH = "../../../"

class NoInternalVisibilityInStdLibTest {
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
                DescriptorToSourceUtils.descriptorToDeclarations(descriptor).firstOrNull()?.getContainingFile() as JetFile
            }
            val byPackage = byFile.keySet().groupBy { it.getPackageFqName() }

            val message = StringBuilder {
                appendln("There are ${internalDescriptors.size} descriptors that have internal visibility:")
                for ((packageFqName, files) in byPackage) {

                    appendln("In package ${packageFqName}:")
                    for (file in files) {

                        appendln("In file ${file.getName()}")
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

    Test fun testJvmStdlib() {
        doTest(ANALYZE_PACKAGE_ROOTS_FOR_JVM, ADDITIONALLY_REQUIRED_PACKAGES_FOR_JVM) {
            val configuration = CompilerConfiguration()
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, "../src/kotlin")
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, "../src/generated")
            configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.getJdkClassesRoots())

            val environment = JetCoreEnvironment.createForProduction(it, configuration)

            val module = TopDownAnalyzerFacadeForJVM.createJavaModule("<module for validating std lib>")
            module.addDependencyOnModule(module)
            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())

            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    environment.getProject(),
                    environment.getSourceFiles(),
                    BindingTraceContext(),
                    { true },
                    module,
                    null,
                    null
            ).getModuleDescriptor()
        }
    }

    Test fun testJsStdlibJar() {
        doTest(ANALYZE_PACKAGE_ROOTS_FOR_JS, ADDITIONALLY_REQUIRED_PACKAGES_FOR_JS) {
            val configuration = CompilerConfiguration()
            val environment = JetCoreEnvironment.createForProduction(it, configuration)
            val project = environment.getProject()
            val pathToJsStdlibJar = KOTLIN_ROOT_PATH + PathUtil.getKotlinPathsForDistDirectory().getJsLibJarPath().path
            val config = LibrarySourcesConfig(project, "testModule", listOf("@", pathToJsStdlibJar), EcmaVersion.defaultVersion(), false, false)

            TopDownAnalyzerFacadeForJS.analyzeFiles(listOf(), { true }, config).getModuleDescriptor()
        }
    }

    private fun doTest(
            testPackages: List<String>,
            additionallyRequiredPackages: List<String>,
            createTestModule: (disposable: Disposable) -> ModuleDescriptor
    ) {
        val disposable = Disposer.newDisposable()
        val module = try {
            createTestModule(disposable)
        }
        finally {
            Disposer.dispose(disposable)
        }

        val requiredPackages = testPackages + additionallyRequiredPackages
        val sink = OutputSink(requiredPackages map { FqName(it) })

        for (testPackage in testPackages) {
            val packageView = module.getPackage(FqName(testPackage))!!
            validateDescriptor(module, packageView, sink)
        }

        sink.reportErrors()
    }

    private fun validateDescriptor(module: ModuleDescriptor, descriptor: DeclarationDescriptor, sink: OutputSink) {
        if (DescriptorUtils.getContainingModule(descriptor) != module) return

        if (descriptor is DeclarationDescriptorWithVisibility) {
            if (descriptor.getVisibility() == Visibilities.INTERNAL) {
                sink.reportInternalVisibility(descriptor)
            }
        }
        when (descriptor) {
            is ClassDescriptor -> descriptor.getDefaultType().getMemberScope().getAllDescriptors().forEach {
                validateDescriptor(module, it, sink)
            }
            is PackageViewDescriptor -> {
                sink.reportValidatedPackage(DescriptorUtils.getFqName(descriptor).toSafe())
                descriptor.getMemberScope().getAllDescriptors().forEach {
                    validateDescriptor(module, it, sink)
                }
            }
        }
    }
}
