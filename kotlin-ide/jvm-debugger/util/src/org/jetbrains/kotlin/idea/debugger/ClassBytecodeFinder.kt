package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.apache.log4j.Logger
import org.jetbrains.kotlin.idea.caches.project.implementingModules
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.io.IOException

class ClassBytecodeFinder(private val project: Project, private val jvmName: JvmClassName, private val file: VirtualFile) {
    private companion object {
        private val LOG = Logger.getLogger(ClassBytecodeFinder::class.java)!!
    }

    private val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file)

    fun find(): ByteArray? = findInCompilerOutput() ?: findInLibraries()

    private fun findInLibraries(): ByteArray? {
        if (!ProjectRootsUtil.isLibrarySourceFile(project, file)) {
            return null
        }

        val classFileName = jvmName.internalName.substringAfterLast('/')
        val fileFinder = getFinderForLibrary()

        for (variant in findTopLevelClassNameVariants()) {
            val variantClassFile = fileFinder.findVirtualFileWithHeader(ClassId.fromString(variant))
            if (variantClassFile != null && variant == jvmName.internalName) {
                return readFile(variantClassFile)
            }

            val packageDir = variantClassFile?.parent
            if (packageDir != null) {
                val classFile = packageDir.findChild("$classFileName.class")
                if (classFile != null) {
                    return readFile(classFile)
                }
            }
        }

        return null
    }

    private fun readFile(file: VirtualFile): ByteArray? {
        try {
            return file.contentsToByteArray(false)
        } catch (e: IOException) {
            // Ensure results are consistent
            LOG.debug("Can't read class file $jvmName", e)
            return null
        }
    }

    private fun getFinderForLibrary(): VirtualFileFinder {
        val fileFinderFactory = VirtualFileFinderFactory.getInstance(project)

        val fileFinders = mutableListOf<VirtualFileFinder>()
        if (module != null) {
            // Prioritize libraries from current module and its dependencies
            fileFinders += fileFinderFactory.create(GlobalSearchScope.moduleWithLibrariesScope(module))
            fileFinders += fileFinderFactory.create(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
        }

        fileFinders += fileFinderFactory.create(GlobalSearchScope.allScope(project))

        return CompoundVirtualFileFinder(fileFinders)
    }

    // User may have classes with dollars in names (e.g. `class Foo$Bar {}`). We have to find them as well.
    private fun findTopLevelClassNameVariants(): List<String> {
        val result = mutableListOf<String>()

        val jdiName = jvmName.internalName.replace('/', '.')
        var index = jdiName.indexOf('$', startIndex = 1)
        while (index >= 0) {
            result += jdiName.take(index)
            index = jdiName.indexOf('$', startIndex = index + 1)
        }

        result += jvmName.internalName
        return result
    }

    private fun findInCompilerOutput(): ByteArray? {
        if (!ProjectRootsUtil.isProjectSourceFile(project, file)) {
            return null
        }

        if (module != null) {
            return findInModuleOutput(module)
        }

        return null
    }

    private fun findInModuleOutput(module: Module): ByteArray? {
        findInSingleModuleOutput(module)?.let { return it }

        for (implementing in module.implementingModules) {
            findInSingleModuleOutput(implementing)?.let { return it }
        }

        return null
    }

    private fun findInSingleModuleOutput(module: Module): ByteArray? {
        for (outputRoot in CompilerPaths.getOutputPaths(arrayOf(module)).toList()) {
            val file = File(outputRoot, jvmName.internalName + ".class")
            if (file.isFile) {
                try {
                    return file.readBytes()
                } catch (e: IOException) {
                    // Ensure results are consistent
                    LOG.debug("Can't read class file $jvmName", e)
                    return null
                }
            }
        }

        return null
    }
}