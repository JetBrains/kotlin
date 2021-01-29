/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.getExportedDependencies
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
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

internal class ObjCExport(val context: Context, symbolTable: SymbolTable) {
    private val target get() = context.config.target

    private val exportedInterface = produceInterface()
    private val codeSpec = exportedInterface?.createCodeSpec(symbolTable)

    private fun produceInterface(): ObjCExportedInterface? {
        if (!target.family.isAppleFamily) return null

        if (!context.config.produce.isFinalBinary) return null

        // TODO: emit RTTI to the same modules as classes belong to.
        //   Not possible yet, since ObjCExport translates the entire "world" API at once
        //   and can't do this per-module, e.g. due to global name conflict resolution.

        val produceFramework = context.config.produce == CompilerOutputKind.FRAMEWORK

        return if (produceFramework) {
            val mapper = ObjCExportMapper(context.frontendServices.deprecationResolver)
            val moduleDescriptors = listOf(context.moduleDescriptor) + context.getExportedDependencies()
            val objcGenerics = context.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
            val namer = ObjCExportNamerImpl(
                    moduleDescriptors.toSet(),
                    context.moduleDescriptor.builtIns,
                    mapper,
                    context.moduleDescriptor.namePrefix,
                    local = false,
                    objcGenerics = objcGenerics
            )
            val headerGenerator = ObjCExportHeaderGeneratorImpl(context, moduleDescriptors, mapper, namer, objcGenerics)
            headerGenerator.translateModule()
            headerGenerator.buildInterface()
        } else {
            null
        }
    }

    lateinit var namer: ObjCExportNamer

    internal fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (context.producedLlvmModuleContainsStdlib) {
            ObjCExportBlockCodeGenerator(codegen).generate()
        }

        if (!context.config.produce.isFinalBinary) return // TODO: emit RTTI to the same modules as classes belong to.

        val mapper = exportedInterface?.mapper ?: ObjCExportMapper()
        namer = exportedInterface?.namer ?: ObjCExportNamerImpl(
                setOf(codegen.context.moduleDescriptor),
                context.moduleDescriptor.builtIns,
                mapper,
                context.moduleDescriptor.namePrefix,
                local = false
        )

        val objCCodeGenerator = ObjCExportCodeGenerator(codegen, namer, mapper)

        if (exportedInterface != null) {
            produceFrameworkSpecific(exportedInterface.headerLines)

            exportedInterface.generateWorkaroundForSwiftSR10177()
        }

        objCCodeGenerator.generate(codeSpec)
        objCCodeGenerator.dispose()
    }

    private fun produceFrameworkSpecific(headerLines: List<String>) {
        val framework = File(context.config.outputFile)
        val frameworkContents = when(target.family) {
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
            |}
        """.trimMargin()

        modules.child("module.modulemap").writeBytes(moduleMap.toByteArray())

        emitInfoPlist(frameworkContents, frameworkName)

        if (target == KonanTarget.MACOS_X64) {
            framework.child("Versions/Current").createAsSymlink("A")
            for (child in listOf(frameworkName, "Headers", "Modules", "Resources")) {
                framework.child(child).createAsSymlink("Versions/Current/$child")
            }
        }
    }

    private fun emitInfoPlist(frameworkContents: File, name: String) {
        val directory = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkContents
            Family.OSX -> frameworkContents.child("Resources").also { it.mkdirs() }
            else -> error(target)
        }

        val file = directory.child("Info.plist")
        val pkg = guessMainPackage() // TODO: consider showing warning if it is root.
        val bundleId = pkg.child(Name.identifier(name)).asString()

        val platform = when (target) {
            KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64 -> "iPhoneOS"
            KonanTarget.IOS_X64 -> "iPhoneSimulator"
            KonanTarget.TVOS_ARM64 -> "AppleTVOS"
            KonanTarget.TVOS_X64 -> "AppleTVSimulator"
            KonanTarget.MACOS_X64 -> "MacOSX"
            KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64 -> "WatchOS"
            KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64 -> "WatchSimulator"
            else -> error(target)
        }
        val properties = context.config.platform.configurables as AppleConfigurables
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
                <string>1.0</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>$platform</string>
                </array>
                <key>CFBundleVersion</key>
                <string>1</string>

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

    // See https://bugs.swift.org/browse/SR-10177
    private fun ObjCExportedInterface.generateWorkaroundForSwiftSR10177() {
        // Code for all protocols from the header should get into the binary.
        // Objective-C protocols ABI is complicated (consider e.g. undocumented extended type encoding),
        // so the easiest way to achieve this (quickly) is to compile a stub by clang.

        val protocolsStub = listOf(
                "__attribute__((used)) static void __workaroundSwiftSR10177() {",
                buildString {
                    append("    ")
                    generatedClasses.forEach {
                        if (it.isInterface) {
                            val protocolName = namer.getClassOrProtocolName(it).objCName
                            append("@protocol($protocolName); ")
                        }
                    }
                },
                "}"
        )

        val source = createTempFile("protocols", ".m").deleteOnExit()
        source.writeLines(headerLines + protocolsStub)

        val bitcode = createTempFile("protocols", ".bc").deleteOnExit()

        val clangCommand = context.config.clang.clangC(
                source.absolutePath,
                "-O2",
                "-emit-llvm",
                "-c", "-o", bitcode.absolutePath
        )

        val result = Command(clangCommand).getResult(withErrors = true)

        if (result.exitCode == 0) {
            context.llvm.additionalProducedBitcodeFiles += bitcode.absolutePath
        } else {
            // Note: ignoring compile errors intentionally.
            // In this case resulting framework will likely be unusable due to compile errors when importing it.
        }
    }

    private fun guessMainPackage(): FqName {
        val allPackages = (context.getIncludedLibraryDescriptors() + context.moduleDescriptor).flatMap {
            it.getPackageFragments() // Includes also all parent packages, e.g. the root one.
        }

        val nonEmptyPackages = allPackages
            .filter { it.getMemberScope().getContributedDescriptors().isNotEmpty() }
            .map { it.fqName }.distinct()

        return allPackages.map { it.fqName }.distinct()
            .filter { candidate -> nonEmptyPackages.all { it.isSubpackageOf(candidate) } }
            // Now there are all common ancestors of non-empty packages. Longest of them is the least common accessor:
            .maxByOrNull { it.asString().length }!!
    }
}
