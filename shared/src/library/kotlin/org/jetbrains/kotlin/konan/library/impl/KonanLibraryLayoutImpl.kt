package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.FileSystem

open class TargetedLibraryLayoutImpl(klib: File, override val target: KonanTarget?) :
    KotlinLibraryLayoutImpl(klib), TargetedKotlinLibraryLayout {

    override val extractingToTemp: TargetedKotlinLibraryLayout by lazy {
        ExtractingTargetedLibraryImpl(this)
    }

    override fun directlyFromZip(zipFileSystem: FileSystem): TargetedKotlinLibraryLayout =
        FromZipTargetedLibraryImpl(this, zipFileSystem)

}

class BitcodeLibraryLayoutImpl(klib: File, target: KonanTarget?) : TargetedLibraryLayoutImpl(klib, target),
    BitcodeKotlinLibraryLayout {
    override val extractingToTemp: BitcodeKotlinLibraryLayout by lazy {
        ExtractingBitcodeLibraryImpl(this)
    }

    override fun directlyFromZip(zipFileSystem: FileSystem): BitcodeKotlinLibraryLayout =
        FromZipBitcodeLibraryImpl(this, zipFileSystem)

}

open class TargetedLibraryAccess<L : KotlinLibraryLayout>(klib: File, val target: KonanTarget?) :
    BaseLibraryAccess<L>(klib) {
    override val layout = TargetedLibraryLayoutImpl(klib, target)
}

open class BitcodeLibraryAccess<L : KotlinLibraryLayout>(klib: File, target: KonanTarget?) :
    TargetedLibraryAccess<L>(klib, target) {
    override val layout = BitcodeLibraryLayoutImpl(klib, target)
}

private open class FromZipTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), TargetedKotlinLibraryLayout

private class FromZipBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipTargetedLibraryImpl(zipped, zipFileSystem), BitcodeKotlinLibraryLayout

open class ExtractingTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl) :
    ExtractingKotlinLibraryLayout(zipped),
    TargetedKotlinLibraryLayout {

    override val includedDir: File by lazy { zipped.extractDir(zipped.includedDir) }
}

class ExtractingBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl) :
    ExtractingTargetedLibraryImpl(zipped), BitcodeKotlinLibraryLayout {

    override val kotlinDir: File by lazy { zipped.extractDir(zipped.kotlinDir) }
    override val nativeDir: File by lazy { zipped.extractDir(zipped.nativeDir) }
}
