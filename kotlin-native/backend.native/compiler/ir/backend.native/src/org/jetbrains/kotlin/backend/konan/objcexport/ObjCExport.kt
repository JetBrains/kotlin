/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.Companion.BUNDLE_ID
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.phases.ErrorReportingContext
import org.jetbrains.kotlin.backend.konan.phases.FrontendContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf

internal class ObjCExportedInterface(
        val generatedClasses: Set<ClassDescriptor>,
        val categoryMembers: Map<ClassDescriptor, List<CallableMemberDescriptor>>,
        val topLevel: Map<SourceFile, List<CallableMemberDescriptor>>,
        val headerLines: List<String>,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
)

internal class ObjCExport(
        val context: FrontendContext,
        private val errorReportingContext: ErrorReportingContext,
        symbolTable: SymbolTable,
        private val config: KonanConfig
) {
    private val target get() = config.target
    private val topLevelNamePrefix get() = config.objCExportTopLevelNamePrefix

    val exportedInterface = produceInterface()
    val codeSpec = exportedInterface?.createCodeSpec(symbolTable)

    var namer: ObjCExportNamer? = null

    private fun produceInterface(): ObjCExportedInterface? {
        if (!target.family.isAppleFamily) return null

        // TODO: emit RTTI to the same modules as classes belong to.
        //   Not possible yet, since ObjCExport translates the entire "world" API at once
        //   and can't do this per-module, e.g. due to global name conflict resolution.

        val produceFramework = config.produce == CompilerOutputKind.FRAMEWORK

        return if (produceFramework) {
            val unitSuspendFunctionExport = config.unitSuspendFunctionObjCExport
            val mapper = ObjCExportMapper(context.frontendServices.deprecationResolver, unitSuspendFunctionExport = unitSuspendFunctionExport)
            val moduleDescriptors = listOf(context.moduleDescriptor) + config.getExportedDependencies(context.moduleDescriptor)
            val objcGenerics = config.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
            val namer = ObjCExportNamerImpl(
                    moduleDescriptors.toSet(),
                    context.moduleDescriptor.builtIns,
                    mapper,
                    topLevelNamePrefix,
                    local = false,
                    objcGenerics = objcGenerics
            )
            val headerGenerator = ObjCExportHeaderGeneratorImpl(
                    config,
                    errorReportingContext,
                    moduleDescriptors,
                    mapper,
                    namer,
                    objcGenerics
            )
            headerGenerator.translateModule()
            headerGenerator.buildInterface()
        } else {
            null
        }
    }

    /**
     * Populate framework directory with headers, module and info.plist.
     */
    fun produceFrameworkInterface() {
        if (exportedInterface != null) {
            produceFrameworkSpecific(exportedInterface.headerLines)
        }
    }

    private fun produceFrameworkSpecific(headerLines: List<String>) {
        val framework = File(config.outputFile)
        val frameworkContents = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> framework

            Family.OSX -> framework.child("Versions/A")
            else -> error(target)
        }

        val headers = frameworkContents.child("Headers")

        val frameworkName = framework.name.removeSuffix(".framework")
        val headerName = frameworkName + ".h"
        val header = headers.child(headerName)
        headers.mkdirs()
        header.writeLines(headerLines)

        val modules = frameworkContents.child("Modules")
        modules.mkdirs()

        val moduleMap = """
            |framework module $frameworkName {
            |    umbrella header "$headerName"
            |
            |    export *
            |    module * { export * }
            |
            |    use Foundation
            |}
        """.trimMargin()

        modules.child("module.modulemap").writeBytes(moduleMap.toByteArray())

        emitInfoPlist(frameworkContents, frameworkName)
        if (target.family == Family.OSX) {
            framework.child("Versions/Current").createAsSymlink("A")
            for (child in listOf(frameworkName, "Headers", "Modules", "Resources")) {
                framework.child(child).createAsSymlink("Versions/Current/$child")
            }
        }
    }

    private fun emitInfoPlist(frameworkContents: File, name: String) {
        val properties = config.platform.configurables as AppleConfigurables

        val directory = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkContents

            Family.OSX -> frameworkContents.child("Resources").also { it.mkdirs() }
            else -> error(target)
        }

        val file = directory.child("Info.plist")
        val bundleId = guessBundleID(name)
        val bundleShortVersionString = config.configuration[BinaryOptions.bundleShortVersionString] ?: "1.0"
        val bundleVersion = config.configuration[BinaryOptions.bundleVersion] ?: "1"
        val platform = properties.platformName()
        val minimumOsVersion = properties.osVersionMin

        val contents = StringBuilder()
        contents.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>$name</string>
                <key>CFBundleIdentifier</key>
                <string>$bundleId</string>
                <key>CFBundleInfoDictionaryVersion</key>
                <string>6.0</string>
                <key>CFBundleName</key>
                <string>$name</string>
                <key>CFBundlePackageType</key>
                <string>FMWK</string>
                <key>CFBundleShortVersionString</key>
                <string>$bundleShortVersionString</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>$platform</string>
                </array>
                <key>CFBundleVersion</key>
                <string>$bundleVersion</string>

        """.trimIndent())

        fun addUiDeviceFamilies(vararg values: Int) {
            val xmlValues = values.joinToString(separator = "\n") {
                "        <integer>$it</integer>"
            }
            contents.append("""
                |    <key>MinimumOSVersion</key>
                |    <string>$minimumOsVersion</string>
                |    <key>UIDeviceFamily</key>
                |    <array>
                |$xmlValues       
                |    </array>

                """.trimMargin())
        }

        // UIDeviceFamily mapping:
        // 1 - iPhone
        // 2 - iPad
        // 3 - AppleTV
        // 4 - Apple Watch
        when (target.family) {
            Family.IOS -> addUiDeviceFamilies(1, 2)
            Family.TVOS -> addUiDeviceFamilies(3)
            Family.WATCHOS -> addUiDeviceFamilies(4)
            else -> {}
        }

        if (target == KonanTarget.IOS_ARM64) {
            contents.append("""
                |    <key>UIRequiredDeviceCapabilities</key>
                |    <array>
                |        <string>arm64</string>
                |    </array>

                """.trimMargin()
            )
        }

        if (target == KonanTarget.IOS_ARM32) {
            contents.append("""
                |    <key>UIRequiredDeviceCapabilities</key>
                |    <array>
                |        <string>armv7</string>
                |    </array>

                """.trimMargin()
            )
        }

        contents.append("""
            </dict>
            </plist>
        """.trimIndent())

        // TODO: Xcode also add some number of DT* keys.

        file.writeBytes(contents.toString().toByteArray())
    }

    private fun guessMainPackage(modules: List<ModuleDescriptor>): FqName? {
        if (modules.isEmpty()) {
            return null
        }

        val allPackages = modules.flatMap {
            it.getPackageFragments() // Includes also all parent packages, e.g. the root one.
        }

        val nonEmptyPackages = allPackages
                .filter { it.getMemberScope().getContributedDescriptors().isNotEmpty() }
                .map { it.fqName }.distinct()

        return allPackages.map { it.fqName }.distinct()
                .filter { candidate -> nonEmptyPackages.all { it.isSubpackageOf(candidate) } }
                // Now there are all common ancestors of non-empty packages. Longest of them is the least common accessor:
                .maxByOrNull { it.asString().length }
    }

    private fun guessBundleID(bundleName: String): String {
        val configuration = config.configuration
        val deprecatedBundleIdOption = configuration[BUNDLE_ID]
        val bundleIdOption = configuration[BinaryOptions.bundleId]
        if (deprecatedBundleIdOption != null && bundleIdOption != null && deprecatedBundleIdOption != bundleIdOption) {
            configuration.report(
                    CompilerMessageSeverity.ERROR,
                    "Both the deprecated -Xbundle-id=<id> and the new -Xbinary=bundleId=<id> options supplied with different values: " +
                            "'$deprecatedBundleIdOption' and '$bundleIdOption'. " +
                            "Please use only one of the options or make sure they have the same value."
            )
        }
        deprecatedBundleIdOption?.let { return it } ?: bundleIdOption?.let { return it }

        // Consider exported libraries only if we cannot infer the package from sources or included libs.
        val mainPackage = guessMainPackage(config.getIncludedLibraryDescriptors(context.moduleDescriptor) + context.moduleDescriptor)
                ?: guessMainPackage(config.getExportedDependencies(context.moduleDescriptor))
                ?: FqName.ROOT

        val bundleID = mainPackage.child(Name.identifier(bundleName)).asString()

        if (mainPackage.isRoot) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Cannot infer a bundle ID from packages of source files and exported dependencies, " +
                            "use the bundle name instead: $bundleName. " +
                            "Please specify the bundle ID explicitly using the -Xbinary=bundleId=<id> compiler flag."
            )
        }
        return bundleID
    }
}
