/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.native.analysis.api.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getAllClassOrObjectSymbols
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.tooling.core.withClosure

interface KtObjCExportFile {
    val fileName: String
    val packageFqName: FqName

    fun KaSession.resolve(): KtResolvedObjCExportFile
}

/**
 * The 'resolved' version of the [KtObjCExportFile].
 * 'Resolved' means that all symbols associated with the file are loaded and available
 * under [classifierSymbols] and [callableSymbols].
 */
data class KtResolvedObjCExportFile(
    val fileName: String,
    val packageFqName: FqName,
    val classifierSymbols: List<KaClassSymbol>,
    val callableSymbols: List<KaCallableSymbol>,
)

/* Factory functions */

fun KtObjCExportFile(file: KtFile): KtObjCExportFile {
    return KtPsiObjCExportFile(file)
}

/**
 * Will read the klib (if any) and returns the list of [KtObjCExportFile] that were found.
 * Returns an empty list if this [KaLibraryModule] is not a klib
 */
fun KaLibraryModule.readKtObjCExportFiles(): List<KtObjCExportFile> {
    val klibAddresses = readKlibDeclarationAddresses() ?: return emptyList()
    return createKtObjCExportFiles(klibAddresses)
}

internal fun createKtObjCExportFiles(addresses: Iterable<KlibDeclarationAddress>): List<KtObjCExportFile> {
    val addressesByPackageName = addresses.groupBy { it.packageFqName }
    return addressesByPackageName.flatMap { (packageName, addresses) ->
        val addressesByFile = addresses.groupBy { it.sourceFileName }
        addressesByFile.mapNotNull { (fileName, addresses) ->
            fileName ?: return@mapNotNull null
            KtKlibObjCExportFile(FileUtil.getNameWithoutExtension(fileName), packageName, addresses.toSet())
        }
    }
}

/* Private Implementations */

private class KtPsiObjCExportFile(
    private val file: KtFile,
) : KtObjCExportFile {
    override val fileName: String
        get() = FileUtil.getNameWithoutExtension(file.name)

    override val packageFqName: FqName
        get() = file.packageFqName

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is KtPsiObjCExportFile) return false
        if (other.file != this.file) return false
        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

    /**
     * See [KtResolvedObjCExportFile]
     */
    override fun KaSession.resolve(): KtResolvedObjCExportFile {
        val symbol = file.symbol
        return KtResolvedObjCExportFile(
            fileName = fileName,
            packageFqName = packageFqName,
            classifierSymbols = getAllClassOrObjectSymbols(symbol),
            callableSymbols = symbol.fileScope.callables.toList()
        )
    }
}

private class KtKlibObjCExportFile(
    override val fileName: String,
    override val packageFqName: FqName,
    private val addresses: Set<KlibDeclarationAddress>,
) : KtObjCExportFile {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is KtKlibObjCExportFile) return false
        if (other.fileName != this.fileName) return false
        if (other.packageFqName != this.packageFqName) return false
        if (other.addresses != this.addresses) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + packageFqName.hashCode()
        result = 31 * result + addresses.hashCode()
        return result
    }

    override fun KaSession.resolve(): KtResolvedObjCExportFile {
        val classifierAddresses = addresses.filterIsInstance<KlibClassAddress>()
        val callableAddresses = addresses.filterIsInstance<KlibCallableAddress>()

        return KtResolvedObjCExportFile(
            fileName = fileName,
            packageFqName = packageFqName,
            classifierSymbols = classifierAddresses
                .mapNotNull { classAddress -> classAddress.getClassOrObjectSymbol() }
                .withClosure<KaClassSymbol> { symbol ->
                    symbol.memberScope.classifiers.filterIsInstance<KaClassSymbol>().asIterable()
                }.toList(),
            callableSymbols = callableAddresses.flatMap { address ->
                address.getCallableSymbols()
            }
        )
    }
}
