package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.FileSystem

open class TargetedLibraryLayoutImpl(klib: File, component: String, override val target: KonanTarget?) :
    KotlinLibraryLayoutImpl(klib, component), TargetedKotlinLibraryLayout {

    override fun directlyFromZip(zipFileSystem: FileSystem): TargetedKotlinLibraryLayout =
        FromZipTargetedLibraryImpl(this, zipFileSystem)

}

open class TargetedLibraryAccess<L : TargetedKotlinLibraryLayout>(klib: File, component: String, val target: KonanTarget?, zipFileSystemAccessor: ZipFileSystemAccessor?) :
    BaseLibraryAccess<L>(klib, component, zipFileSystemAccessor) {

    override val layout = TargetedLibraryLayoutImpl(klib, component, target)
    protected open val extractingLayout: TargetedKotlinLibraryLayout by lazy { ExtractingTargetedLibraryImpl(layout) }

    @Suppress("UNCHECKED_CAST")
    fun <T> realFiles(action: (L) -> T): T =
        if (layout.isZipped)
            action(extractingLayout as L)
        else
            action(layout as L)
}

private open class FromZipTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl, zipFileSystem: FileSystem) :
    FromZipBaseLibraryImpl(zipped, zipFileSystem), TargetedKotlinLibraryLayout

private open class ExtractingTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl) : TargetedKotlinLibraryLayout {
    override val libFile: File get() = error("Extracting layout doesn't extract its own root")
    override val libraryName = zipped.libraryName
    override val component = zipped.component

    override val includedDir: File by lazy { extractDir(zipped.klib, zipped.includedDir) }
}

private fun extractDir(zipFile: File, directory: File): File {
    val directoryInZipExists = zipFile.withZipFileSystem { zipFileSystem -> zipFileSystem.file(directory).isDirectory }
    return if (directoryInZipExists) {
        val extractedDir = createTempDir(directory.name)
        zipFile.unzipTo(extractedDir, fromSubdirectory = directory)
        extractedDir.deleteOnExitRecursively()
        extractedDir
    } else {
        // return a deliberately nonexistent directory name
        File(zipFile.path + "!" + directory.path)
    }
}
