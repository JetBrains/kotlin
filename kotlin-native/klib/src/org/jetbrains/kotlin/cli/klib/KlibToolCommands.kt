/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.DumpIrReferenceRenderingAsSignatureStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.library.BitcodeLibrary
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.hasAbi
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.library.nativeTargets
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.util.*
import org.jetbrains.kotlin.metadata.ProtoBuf.PackageFragment as PackageFragmentProto

internal sealed class KlibToolCommand(
    protected val output: KlibToolOutput,
    protected val args: KlibToolArguments,
) {
    abstract fun execute()

    protected fun checkLibraryHasIr(library: KotlinLibrary): Boolean {
        if (!library.hasMainIr) {
            output.logError("Library ${library.libraryFile} is an IR-less library")
            return false
        }
        return true
    }

    protected fun KotlinIrSignatureVersion?.checkSupportedInLibrary(library: KotlinLibrary): Boolean {
        if (this != null) {
            val supportedSignatureVersions = library.versions.irSignatureVersions
            if (this !in supportedSignatureVersions) {
                output.logError(
                    "Signature version ${this.number} is not supported in library ${library.libraryFile}." +
                                        " Supported versions: ${supportedSignatureVersions.joinToString { it.number.toString() }}"
                )
                return false
            }
        }
        return true
    }

    protected fun KotlinIrSignatureVersion?.getMostSuitableSignatureRenderer(): IdSignatureRenderer? = when (this) {
        KotlinIrSignatureVersion.V1 -> IdSignatureRenderer.LEGACY
        null, KotlinIrSignatureVersion.V2 -> IdSignatureRenderer.DEFAULT
        else -> {
            output.logError("Unsupported signature version: $number")
            null
        }
    }

    /**
     * Note that [libraryPath] can be either absolute, or relative to the current working directory.
     * Other options are not supported.
     */
    protected fun resolveKlib(libraryPath: String): KotlinLibrary =
        klibResolver(distributionKlib = null, skipCurrentDir = false, KlibToolLogger(output)).resolve(libraryPath)
}

internal class Info(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    override fun execute() {
        val library = resolveKlib(args.libraryPath)
        val metadataHeader = parseModuleHeader(library.moduleHeaderData)

        val nonEmptyPackageFQNs = buildSet {
            addAll(metadataHeader.packageFragmentNameList)
            removeAll(metadataHeader.emptyPackageList)

            // Sometimes `emptyPackageList` is empty, so it's necessary to explicitly filter out empty packages:
            val stillRemainingEmptyPackageFQNs = filterTo(hashSetOf()) { packageName ->
                library.packageMetadataParts(packageName).all { partName ->
                    parsePackageFragment(library.packageMetadata(packageName, partName)).isEmpty()
                }
            }

            removeAll(stillRemainingEmptyPackageFQNs)
        }.sorted()

        val manifestProperties: SortedMap<String, String> = library.manifestProperties.entries
            .associateTo(sortedMapOf()) { it.key.toString() to it.value.toString() }

        output.appendLine("Full path: ${library.libraryFile.canonicalPath}")
        output.appendLine("Module name (metadata): ${metadataHeader.moduleName}")
        output.appendLine("Non-empty package FQNs (${nonEmptyPackageFQNs.size}):")
        nonEmptyPackageFQNs.forEach { packageFQN ->
            output.appendLine("  $packageFQN")
        }
        output.appendLine("Has IR: ${library.hasMainIr}")
        val irInfo = KlibIrInfoLoader(library).loadIrInfo()
        irInfo?.preparedInlineFunctionCopyNumber?.let { output.appendLine("  Inlinable function copies: $it") }
        output.appendLine("Has FileEntries table: ${library.hasFileEntriesTable}")
        output.appendLine("Has LLVM bitcode: ${library.hasBitcode}")
        output.appendLine("Has ABI: ${library.hasAbi}")
        output.appendLine("Manifest properties:")
        manifestProperties.entries.forEach { (key, value) ->
            output.appendLine("  $key=$value")
        }
        library.loadSizeInfo()?.renderTo(output)
    }

    companion object {
        private fun PackageFragmentProto.isEmpty(): Boolean = when {
            class_List.isNotEmpty() -> false
            !hasPackage() -> true
            else -> `package`.functionList.isEmpty() && `package`.propertyList.isEmpty() && `package`.typeAliasList.isEmpty()
        }

        private val KotlinLibrary.hasBitcode: Boolean
            get() {
                if (this is BitcodeLibrary) {
                    val componentName = componentList.firstOrNull() ?: return false

                    for (nativeTargetName in nativeTargets) {
                        val nativeTarget = KonanTarget.predefinedTargets[nativeTargetName] ?: continue
                        val targetedLibrary = createKonanLibrary(
                            libraryFilePossiblyDenormalized = libraryFile,
                            component = componentName,
                            target = nativeTarget,
                        )

                        return targetedLibrary.bitcodePaths.isNotEmpty()
                    }
                }

                return false
            }

        private fun KlibElementWithSize.renderTo(appendable: Appendable, indent: Int = 0) {
            appendable.appendLine("  ".repeat(indent) + name + ": " + prettySize())
            children.forEach { it.renderTo(appendable, indent + 1) }
        }

        private fun KlibElementWithSize.prettySize(): String {
            val sizeRawString = (size / 1024).toString()
            val sizeDotSeparatedString = sizeRawString.reversed().chunked(3).joinToString(".").reversed()
            return "$sizeDotSeparatedString KB"
        }
    }
}

internal class DumpIr(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun execute() {
        val library = resolveKlib(args.libraryPath)

        if (!checkLibraryHasIr(library)) return

        if (args.signatureVersion != null && args.signatureVersion != KotlinIrSignatureVersion.V2) {
            // TODO: support passing any signature version through `DumpIrTreeOptions`, KT-62828
            output.logWarning("using a non-default signature version in \"dump-ir\" is not supported yet")
        }

        val module = ModuleDescriptorLoader(output).load(library)

        val idSignaturer = KonanIdSignaturer(KonanManglerDesc)
        val symbolTable = SymbolTable(idSignaturer, IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, ModuleDescriptorLoader.languageVersionSettings, module)
        val irBuiltIns = IrBuiltInsOverDescriptors(module.builtIns, typeTranslator, symbolTable)

        val linker = KlibToolIrLinker(output, module, irBuiltIns, symbolTable)
        module.allDependencyModules.forEach {
            linker.deserializeOnlyHeaderModule(it, it.kotlinLibrary)
            linker.resolveModuleDeserializer(it, null).init()
        }
        val irFragment = linker.deserializeFullModule(module, library)
        linker.resolveModuleDeserializer(module, null).init()
        linker.modulesWithReachableTopLevels.forEach(IrModuleDeserializer::deserializeReachableDeclarations)

        val dumpOptions = DumpIrTreeOptions(
            printSignatures = true,
            referenceRenderingStrategy = DumpIrReferenceRenderingAsSignatureStrategy(KonanManglerIr)
        )

        output.append(irFragment.dump(dumpOptions))
    }
}

internal class DumpIrInlinableFunctions(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun execute() {
        val library = resolveKlib(args.libraryPath)

        if (!checkLibraryHasIr(library)) return

        if (!library.hasInlinableFunsIr) {
            output.appendLine("// No inlinable functions in ${library.libraryFile}")
            return
        }

        if (args.signatureVersion != null && args.signatureVersion != KotlinIrSignatureVersion.V2) {
            // TODO: support passing any signature version through `DumpIrTreeOptions`, KT-62828
            output.logWarning("using a non-default signature version in \"dump-ir-inlinable-functions\" is not supported yet")
        }

        val module = ModuleDescriptorLoader(output).load(library)

        val idSignaturer = KonanIdSignaturer(KonanManglerDesc)
        val symbolTable = SymbolTable(idSignaturer, IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, ModuleDescriptorLoader.languageVersionSettings, module)
        val irBuiltIns = IrBuiltInsOverDescriptors(module.builtIns, typeTranslator, symbolTable)

        val moduleDeserializer = NonLinkingIrInlineFunctionDeserializer.ModuleDeserializer(
                library = library,
                detachedSymbolTable = symbolTable,
                irInterner = IrInterningService(),
                irBuiltIns = irBuiltIns,
        )

        val dummyIrFile = IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(name = "<unknown>"),
                symbol = IrFileSymbolImpl(),
                packageFqName = FqName.ROOT
        )

        val dumpOptions = DumpIrTreeOptions(
                printSignatures = true,
                referenceRenderingStrategy = DumpIrReferenceRenderingAsSignatureStrategy(KonanManglerIr)
        )

        val irDumps: List<String> = moduleDeserializer.reversedSignatureIndex.keys.mapNotNull { signature: IdSignature ->
            val preprocessedFunction = moduleDeserializer.deserializeInlineFunction(signature, dummyIrFile)
                    ?: return@mapNotNull null
            val irDump = preprocessedFunction.dump(dumpOptions)
            val irDumpFirstLine = irDump.substringBefore(Printer.LINE_SEPARATOR)
            irDumpFirstLine to irDump
        }.sortedBy { /* irDumpFirstLine */ it.first }.map { /* irDump */ it.second }

        output.appendLine("// ${irDumps.size} inlinable functions in ${library.libraryFile}")

        for (irDump in irDumps) {
            output.appendLine(irDump)
        }
    }
}

internal class DumpAbi(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    @OptIn(ExperimentalLibraryAbiReader::class)
    override fun execute() {
        val library = resolveKlib(args.libraryPath)

        if (!checkLibraryHasIr(library)) return

        val abiSignatureVersion = args.signatureVersion?.let { signatureVersion ->
            if (!signatureVersion.checkSupportedInLibrary(library)) return

            val abiSignatureVersion = AbiSignatureVersion.resolveByVersionNumber(signatureVersion.number)
            if (!abiSignatureVersion.isSupportedByAbiReader) {
                output.logError(
                    "Signature version ${signatureVersion.number} is not supported by the KLIB ABI reader." +
                            " Supported versions: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )
                return
            }

            abiSignatureVersion
        } ?: run {
            val versionsSupportedByAbiReader: Map<Int, AbiSignatureVersion> = AbiSignatureVersion.allSupportedByAbiReader
                .associateBy { it.versionNumber }

            val abiSignatureVersion = library.versions.irSignatureVersions
                .map { it.number }
                .sortedDescending()
                .firstNotNullOfOrNull { versionsSupportedByAbiReader[it] }

            if (abiSignatureVersion == null) {
                output.logError(
                    "There is no signature version that would be both supported in library ${library.libraryFile}" +
                            " and by the KLIB ABI reader. Supported versions in the library:" +
                            " ${library.versions.irSignatureVersions.joinToString { it.number.toString() }}" +
                            ". Supported versions by the KLIB ABI reader: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )
                return
            }

            abiSignatureVersion
        }

        LibraryAbiRenderer.render(
            libraryAbi = LibraryAbiReader.readAbiInfo(File(library.libraryFile.absolutePath)),
            output = output,
            settings = AbiRenderingSettings(
                renderedSignatureVersion = abiSignatureVersion,
                renderManifest = false,
                renderDeclarations = true,
                indentationString = "    ",

                )
        )
    }
}

internal class DumpMetadata(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    override fun execute() {
        val idSignatureRenderer: IdSignatureRenderer? = runIf(args.printSignatures) {
            args.signatureVersion.getMostSuitableSignatureRenderer() ?: return
        }
        KotlinpBasedMetadataDumper(output, idSignatureRenderer).dumpLibrary(resolveKlib(args.libraryPath), args.testMode)
    }
}

internal class DumpMetadataSignatures(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    override fun execute() {
        // Don't call `checkSupportedInLibrary()` - the signatures are anyway generated on the fly.

        val idSignatureRenderer = args.signatureVersion.getMostSuitableSignatureRenderer() ?: return

        val module = ModuleDescriptorLoader(output).load(resolveKlib(args.libraryPath))

        DescriptorSignaturesRenderer(output, idSignatureRenderer).render(module)
    }
}

internal class DumpIrSignatures(output: KlibToolOutput, args: KlibToolArguments) : KlibToolCommand(output, args) {
    override fun execute() {
        val library = resolveKlib(args.libraryPath)

        if (!checkLibraryHasIr(library) || !args.signatureVersion.checkSupportedInLibrary(library)) return

        val idSignatureRenderer = args.signatureVersion.getMostSuitableSignatureRenderer() ?: return

        val signatures = IrSignaturesExtractor(library).extract()
        IrSignaturesRenderer(output, idSignatureRenderer).render(signatures)
    }
}
