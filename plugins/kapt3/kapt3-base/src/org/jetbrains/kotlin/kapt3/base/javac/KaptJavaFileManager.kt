/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.javac

import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.Option
import com.sun.tools.javac.util.Context
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URI
import java.util.*
import javax.tools.*

class KaptJavaFileManager(context: Context, private val shouldRecordFileRead: Boolean) : JavacFileManager(context, true, null) {
    private val fileReadHistory = mutableSetOf<URI>()

    fun handleOptionJavac9(option: Option, value: String) {
        val handleOptionMethod = JavacFileManager::class.java
            .getMethod("handleOption", Option::class.java, String::class.java)

        handleOptionMethod.invoke(this, option, value)
    }

    var rootsToFilter = emptySet<File>()
    // TODO (gavra): store these more efficiently by package (requires some changes to type collection).
    var typeToIgnore = emptySet<String>()

    override fun list(
        location: JavaFileManager.Location?,
        packageName: String?,
        kinds: MutableSet<JavaFileObject.Kind>?,
        recurse: Boolean
    ): MutableIterable<JavaFileObject> {
        val originalList = super.list(location, packageName, kinds, recurse)

        val shouldPackageBeFiltered =
            location != null &&
                    location == StandardLocation.CLASS_PATH &&
                    rootsToFilter.isNotEmpty() &&
                    typeToIgnore.isNotEmpty() &&
                    filterThisPath(packageName)

        val filteredList = LinkedList<JavaFileObject>()
        for (file in originalList)
            if (!shouldPackageBeFiltered || !shouldBeFiltered(packageName, file)) {
                filteredList.add(wrapWithReadMonitoringIfNeeded(location, file) as JavaFileObject)
            }

        return filteredList
    }

    override fun getJavaFileForInput(location: JavaFileManager.Location?, className: String?, kind: JavaFileObject.Kind?): JavaFileObject? {
        val file = super.getJavaFileForInput(location, className, kind)
        return wrapWithReadMonitoringIfNeeded(location, file) as JavaFileObject?
    }

    override fun getJavaFileForOutput(
        location: JavaFileManager.Location?,
        className: String?,
        kind: JavaFileObject.Kind?,
        sibling: FileObject?
    ): JavaFileObject? {
        val file = super.getJavaFileForOutput(location, className, kind, sibling)
        return wrapWithReadMonitoringIfNeeded(location, file) as JavaFileObject?
    }

    override fun getFileForInput(location: JavaFileManager.Location?, packageName: String?, relativeName: String?): FileObject? {
        val file = super.getFileForInput(location, packageName, relativeName)
        return wrapWithReadMonitoringIfNeeded(location, file)
    }

    override fun getFileForOutput(
        location: JavaFileManager.Location?,
        packageName: String?,
        relativeName: String?,
        sibling: FileObject?
    ): FileObject? {
        val file = super.getFileForOutput(location, packageName, relativeName, sibling)
        return wrapWithReadMonitoringIfNeeded(location, file)
    }

    /** javac does not play nice with wrapped file objects in this method; so we unwrap */
    override fun inferBinaryName(location: JavaFileManager.Location?, file: JavaFileObject): String? =
        super.inferBinaryName(location, unwrapObject(file) as JavaFileObject)

    /** javac does not play nice with wrapped file objects in this method; so we unwrap */
    override fun isSameFile(a: FileObject, b: FileObject): Boolean {
        return super.isSameFile(unwrapObject(a), unwrapObject(b))
    }

    private fun wrapWithReadMonitoringIfNeeded(location: JavaFileManager.Location?, file: FileObject?) =
        if (shouldRecordFileRead && location != StandardLocation.ANNOTATION_PROCESSOR_PATH && file != null && file is JavaFileObject)
            ReadMonitoredJavaFileObject(file)
        else file

    private fun unwrapObject(file: FileObject): FileObject =
        if (file is ReadMonitoredJavaFileObject) file.getJavaFileObject() else file

    fun renderFileReadHistory() = fileReadHistory.sorted().joinToString("\n")

    private fun filterThisPath(packageName: String?): Boolean {
        packageName ?: return false

        val relativePath = packageName.replace('.', File.separatorChar)
        return rootsToFilter.any { it.resolve(relativePath).isDirectory }
    }

    private fun shouldBeFiltered(packageName: String?, fileObject: JavaFileObject): Boolean {
        if (fileObject.kind != JavaFileObject.Kind.CLASS) return false
        return when (fileObject.toUri().scheme) {
            "jar", "zip" -> false
            else -> {
                val typeName = packageName?.let { "$it." } + File(fileObject.toUri()).name.dropLast(".class".length)

                return typeToIgnore.contains(typeName)
            }
        }
    }

    private inner class ReadMonitoredJavaFileObject(innerFile: JavaFileObject) : ForwardingJavaFileObject<JavaFileObject>(innerFile) {
        fun getJavaFileObject(): JavaFileObject = fileObject
        override fun toString(): String = fileObject.name
        override fun openInputStream(): InputStream {
            recordFileRead()
            return super.openInputStream()
        }

        override fun openReader(ignoreEncodingErrors: Boolean): Reader {
            recordFileRead()
            return super.openReader(ignoreEncodingErrors)
        }

        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
            recordFileRead()
            return super.getCharContent(ignoreEncodingErrors)
        }

        private fun recordFileRead() {
            fileReadHistory.add(fileObject.toUri())
        }
    }

    companion object {
        internal fun preRegister(context: Context, shouldRecordFileRead: Boolean) {
            context.put(JavaFileManager::class.java, Context.Factory { KaptJavaFileManager(it, shouldRecordFileRead) })
        }
    }
}