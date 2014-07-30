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
import org.jetbrains.jet.config.CommonConfigurationKeys
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.resolve.name.Name
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
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.utils.PathUtil
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.junit.Assert
import java.util.HashSet
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

// this list is not designed to contain all packages, it is need for sanity check in case test code breaks
private val PACKAGES_SHOULD_BE_VALIDATED = listOf("kotlin", "kotlin.concurrent", "kotlin.jvm") map { FqName(it) }

class NoInternalVisibilityInStdLibTest {
    private class OutputSink {
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

            Assert.assertTrue(
                    "Some of the expected stdlib packages were not validated, check code of the test",
                    validatedPackages.containsAll(PACKAGES_SHOULD_BE_VALIDATED)
            )

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

    Test fun testNoInternalVisibility() {
        val disposable = Disposer.newDisposable()
        val module = try {
            val configuration = CompilerConfiguration()
            configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, "../src/kotlin")
            configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.getJdkClassesRoots())
            val environment = JetCoreEnvironment.createForProduction(disposable, configuration)
            val module = AnalyzerFacadeForJVM.createJavaModule("<module for validating std lib>")
            module.addDependencyOnModule(module)
            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
            AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    environment.getProject(),
                    environment.getSourceFiles(),
                    BindingTraceContext(),
                    { true },
                    module,
                    null,
                    null
            ).getModuleDescriptor()
        }
        finally {
            Disposer.dispose(disposable)
        }
        val kotlinPackage = module.getPackage(FqName("kotlin"))!!
        val sink = OutputSink()
        validateDescriptor(module, kotlinPackage, sink)
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