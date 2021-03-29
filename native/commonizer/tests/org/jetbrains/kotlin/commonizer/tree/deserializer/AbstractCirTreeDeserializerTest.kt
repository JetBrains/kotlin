/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import com.intellij.openapi.util.io.FileUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.commonizer.mergedtree.CirFictitiousFunctionClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.*
import org.jetbrains.kotlin.commonizer.utils.MockModulesProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class AbstractCirTreeDeserializerTest : KtUsefulTestCase() {
    annotation class ModuleBuilderDsl

    data class SourceFile(val name: String, @Language("kotlin") val content: String)

    data class Module(val name: String, val sourceFiles: List<SourceFile>)

    @ModuleBuilderDsl
    class ModuleBuilder {
        var name: String = "test-module"
        var sourceFiles: List<SourceFile> = emptyList()

        fun source(name: String = "test.kt", @Language("kotlin") content: String) {
            sourceFiles = sourceFiles + SourceFile(name, content)
        }

        fun build() = Module(name, sourceFiles.toList())
    }

    @ModuleBuilderDsl
    fun module(builder: ModuleBuilder.() -> Unit): Module {
        return ModuleBuilder().also(builder).build()
    }

    fun deserializeModule(builder: ModuleBuilder.() -> Unit): CirTreeModule {
        val module = module(builder)
        val moduleFolder = FileUtil.createTempDirectory(module.name, null)
        module.sourceFiles.forEach { sourceFile ->
            moduleFolder.resolve(sourceFile.name).writeText(sourceFile.content)
        }

        val moduleDescriptor = createModuleDescriptor(moduleFolder, module)
        val metadata = MockModulesProvider.SERIALIZER.serializeModule(moduleDescriptor)
        val typeResolver = CirTypeResolver.create(
            CirProvidedClassifiers.of(
                CirFictitiousFunctionClassifiers,
                CirProvidedClassifiers.by(MockModulesProvider.create(moduleDescriptor)),
                CirProvidedClassifiers.by(MockModulesProvider.create(DefaultBuiltIns.Instance.builtInsModule))
            )
        )
        return defaultCirTreeModuleDeserializer(metadata, typeResolver)
    }

    fun deserializeSourceFile(@Language("kotlin") sourceFileContent: String): CirTreeModule {
        return deserializeModule {
            source("test.kt", content = sourceFileContent)
        }
    }


    private fun createModuleDescriptor(moduleRoot: File, module: Module): ModuleDescriptor {
        check(Name.isValidIdentifier(module.name))
        val configuration = KotlinTestUtils.newConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name)

        val environment: KotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
            parentDisposable = testRootDisposable,
            initialConfiguration = configuration,
            extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
        )

        val psiFactory = KtPsiFactory(environment.project)

        val psiFiles: List<KtFile> = moduleRoot.walkTopDown()
            .filter { it.isFile }
            .map { psiFactory.createFile(it.name, KtTestUtil.doLoadFile(it)) }
            .toList()

        return CommonResolverForModuleFactory.analyzeFiles(
            files = psiFiles,
            moduleName = Name.special("<${module.name}>"),
            dependOnBuiltIns = true,
            languageVersionSettings = environment.configuration.languageVersionSettings,
            targetPlatform = CommonPlatforms.defaultCommonPlatform,
            targetEnvironment = CompilerEnvironment,
            dependenciesContainer = DependenciesContainerImpl(),
        ) { content ->
            environment.createPackagePartProvider(content.moduleContentScope)
        }.moduleDescriptor
    }

    private class DependenciesContainerImpl : CommonDependenciesContainer {
        private object DefaultBuiltInsModuleInfo : ModuleInfo {
            override val name get() = DefaultBuiltIns.Instance.builtInsModule.name
            override fun dependencies() = listOf(this)
            override fun dependencyOnBuiltIns() = ModuleInfo.DependencyOnBuiltIns.LAST
            override val platform get() = CommonPlatforms.defaultCommonPlatform
            override val analyzerServices get() = CommonPlatformAnalyzerServices
        }

        override val moduleInfos: List<ModuleInfo> get() = listOf(DefaultBuiltInsModuleInfo)
        override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
        override val refinesModuleInfos: List<ModuleInfo> get() = emptyList()
        override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
        override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null

        override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor {
            check(moduleInfo == DefaultBuiltInsModuleInfo) { "Unknown module info $moduleInfo" }
            return DefaultBuiltIns.Instance.builtInsModule
        }
    }

    protected fun CirTreeModule.assertSinglePackage(): CirTreePackage {
        return packages.singleOrNull()
            ?: kotlin.test.fail("Expected single package. Found ${packages.map { it.pkg.packageName }}")
    }

    protected fun CirTreeModule.assertSingleProperty(): CirTreeProperty {
        return assertSinglePackage().properties.singleOrNull()
            ?: kotlin.test.fail("Expected single property. Found ${assertSinglePackage().properties.map { it.property.name }}")
    }

    protected fun CirTreeModule.assertSingleFunction(): CirTreeFunction {
        return assertSinglePackage().functions.singleOrNull()
            ?: kotlin.test.fail("Expected single property. Found ${assertSinglePackage().functions.map { it.function.name }}")
    }

    protected fun CirTreeModule.assertSingleClass(): CirTreeClass {
        return assertSinglePackage().classes.singleOrNull()
            ?: kotlin.test.fail("Expected single class. Found ${assertSinglePackage().classes.map { it.clazz.name }}")
    }

    protected fun CirTreeModule.assertSingleTypeAlias(): CirTreeTypeAlias {
        return assertSinglePackage().typeAliases.singleOrNull()
            ?: kotlin.test.fail("Expected single type alias. Found ${assertSinglePackage().typeAliases.map { it.typeAlias.name }}")
    }
}
