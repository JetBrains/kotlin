package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.FileSystem

open class TargetedLibraryLayoutImpl(klib: File, component: String, override val target: KonanTarget?) :
    KotlinLibraryLayoutImpl(klib, component), TargetedKotlinLibraryLayout {

    override fun directlyFromZip(zipFileSystem: FileSystem): TargetedKotlinLibraryLayout =
        FromZipTargetedLibraryImpl(this, zipFileSystem)

}

class BitcodeLibraryLayoutImpl(klib: File, component: String, target: KonanTarget?) :
    TargetedLibraryLayoutImpl(klib, component, target), BitcodeKotlinLibraryLayout {

    override fun directlyFromZip(zipFileSystem: FileSystem): BitcodeKotlinLibraryLayout =
        FromZipBitcodeLibraryImpl(this, zipFileSystem)

}

open class TargetedLibraryAccess<L : TargetedKotlinLibraryLayout>(klib: File, component: String, val target: KonanTarget?) :
    BaseLibraryAccess<L>(klib, component) {

    override val layout = TargetedLibraryLayoutImpl(klib, component, target)
    protected open val extractingLayout: TargetedKotlinLibraryLayout by lazy { ExtractingTargetedLibraryImpl(layout) }

    @Suppress("UNCHECKED_CAST")
    fun <T> realFiles(action: (L) -> T): T =
        if (layout.isZipped)
            action(extractingLayout as L)
        else
            action(layout as L)
}

open class BitcodeLibraryAccess<L : BitcodeKotlinLibraryLayout>(klib: File, component: String, target: KonanTarget?) :
    TargetedLibraryAccess<L>(klib, component, target) {

    override val layout = BitcodeLibraryLayoutImpl(klib, component, target)
    override val extractingLayout: BitcodeKotlinLibraryLayout by lazy { ExtractingBitcodeLibraryImpl(layout) }
}

private open class FromZipTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), TargetedKotlinLibraryLayout

private class FromZipBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipTargetedLibraryImpl(zipped, zipFileSystem), BitcodeKotlinLibraryLayout

private open class ExtractingTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl) : TargetedKotlinLibraryLayout {
    override val libFile: File get() = error("Extracting layout doesn't extract its own root")
    override val libraryName = zipped.libraryName
    override val component = zipped.component

    override val includedDir: File by lazy { zipped.extractDir(zipped.includedDir) }
}

private class ExtractingBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl) :
    ExtractingTargetedLibraryImpl(zipped), BitcodeKotlinLibraryLayout {

    override val nativeDir: File by lazy { zipped.extractDir(zipped.nativeDir) }
}
