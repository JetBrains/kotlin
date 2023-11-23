/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.konan.KlibFactories
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.Companion.KONAN_HOME
import org.jetbrains.kotlin.backend.konan.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportHeaderGeneratorImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createFlexiblePhaseConfig
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.MockProjectEx
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

object Fe10ObjCExportHeaderGenerator : AbstractObjCExportHeaderGeneratorTest.ObjCExportHeaderGenerator {
    override fun generateHeaders(disposable: Disposable, root: File): String {
        val headerGenerator = createObjCExportHeaderGenerator(disposable, root)
        headerGenerator.translateModuleDeclarations()
        return headerGenerator.build().joinToString(System.lineSeparator())
    }

    private fun createObjCExportHeaderGenerator(disposable: Disposable, root: File): ObjCExportHeaderGenerator {
        val mapper = ObjCExportMapper(
            unitSuspendFunctionExport = UnitSuspendFunctionObjCExport.DEFAULT
        )

        val namer = ObjCExportNamerImpl(
            mapper = mapper,
            builtIns = DefaultBuiltIns.Instance,
            local = false,
            problemCollector = ObjCExportProblemCollector.SILENT,
            configuration = object : ObjCExportNamer.Configuration {
                override val topLevelNamePrefix: String get() = ""
                override fun getAdditionalPrefix(module: ModuleDescriptor): String? = null
                override val objcGenerics: Boolean = true
            }
        )

        val environment: KotlinCoreEnvironment = createKotlinCoreEnvironment(disposable)
        val phaseContext = BasicPhaseContext(
            KonanConfig(environment.project, environment.configuration)
        )

        val kotlinFiles = root.walkTopDown().filter { it.isFile }.filter { it.extension == "kt" }.toList()

        return ObjCExportHeaderGeneratorImpl(
            context = phaseContext,
            moduleDescriptors = listOf(createModuleDescriptor(environment, kotlinFiles)),
            mapper = mapper,
            namer = namer,
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true
        )
    }
}