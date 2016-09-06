/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.annotation.processing.impl

import java.io.File
import javax.annotation.processing.Filer
import javax.lang.model.element.Element
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class KotlinFiler(
        val generatedSourceDir: File, 
        val classesOutputDir: File,
        internal var onFileCreatedHandler: (File) -> Unit = {}
) : Filer {
    private companion object {
        val PACKAGE_INFO_SUFFIX = ".packageInfo"
    }
    
    internal var wasAnythingGenerated: Boolean = false
        private set
    
    private fun getGeneratedFile(nameCharSequence: CharSequence, extension: String, baseDir: File = generatedSourceDir): Pair<File, String> {
        val name = nameCharSequence.toString()
        val isPackageInfo = name.endsWith(PACKAGE_INFO_SUFFIX)
        val fqName = if (isPackageInfo) name.substring(0, PACKAGE_INFO_SUFFIX.length) else name

        val packageName = fqName.substringBeforeLast('.', "")
        
        val packageDir = if (packageName.isNotEmpty()) {
            File(baseDir, packageName.replace('.', '/')).apply { mkdirs() }
        } 
        else {
            baseDir
        }
        
        val fileName = fqName.substringAfterLast('.') + (if (isPackageInfo) PACKAGE_INFO_SUFFIX else extension)
        val file = File(packageDir, fileName)
        return Pair(file, file.toRelativeString(baseDir))
    }
    
    private fun File.notifyCreated() = apply {
        wasAnythingGenerated = true
        onFileCreatedHandler(this)
    }
    
    override fun createSourceFile(name: CharSequence, vararg originatingElements: Element?): JavaFileObject {
        val (file, fileName) = getGeneratedFile(name, ".java")
        return KotlinJavaFileObject(file.notifyCreated(), fileName)
    }

    override fun getResource(location: JavaFileManager.Location, pkg: CharSequence, relativeName: CharSequence): FileObject? {
        val (file, fileName) = getResourceFile(location, pkg, relativeName)
        return KotlinFileObject(file, fileName)
    }

    override fun createResource(
            location: JavaFileManager.Location,
            pkg: CharSequence,
            relativeName: CharSequence,
            vararg originatingElements: Element?
    ): FileObject? {
        val (resourceFile, fileName) = getResourceFile(location, pkg, relativeName)
        resourceFile.parentFile.mkdirs()
        return KotlinFileObject(resourceFile.notifyCreated(), fileName)
    }
    
    private fun getResourceFile(location: JavaFileManager.Location, pkg: CharSequence, relativeName: CharSequence): Pair<File, String> {
        val baseDir = when (location) {
            StandardLocation.CLASS_OUTPUT -> classesOutputDir
            StandardLocation.SOURCE_OUTPUT -> generatedSourceDir
            else -> throw IllegalArgumentException("Location is not supported: $location (${location.name})")
        }

        val targetDir = File(baseDir, pkg.toString().replace('.', '/'))
        targetDir.mkdirs()

        val resourceFile = File(targetDir, relativeName.toString())
        return Pair(resourceFile, resourceFile.toRelativeString(baseDir))
    }

    override fun createClassFile(name: CharSequence, vararg originatingElements: Element?): JavaFileObject {
        val (file, fileName) = getGeneratedFile(name, ".class")
        return KotlinJavaFileObject(file.notifyCreated(), fileName)
    }
}