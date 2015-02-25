/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.lang.resolve.android

import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.io.File
import javax.xml.parsers.SAXParserFactory
import java.io.File
import java.io.FileInputStream
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetFile
import javax.xml.parsers.SAXParser
import java.util.HashMap
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentLinkedQueue
import com.intellij.testFramework.LightVirtualFile
import com.intellij.psi.PsiManager
import java.io.FileInputStream
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.impl.PsiModificationTrackerImpl
import java.util.Queue
import com.intellij.psi.PsiFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider.Result
import java.util.concurrent.atomic.AtomicInteger
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.impl.*
import com.intellij.openapi.vfs.*
import kotlin.properties.*

public abstract class AndroidUIXmlProcessor(protected val project: Project) {

    public class NoAndroidManifestFound : Exception("No android manifest file found in project root")

    private val androidImports = listOf(
            "android.app.Activity",
            "android.view.View",
            "android.widget.*")

    public abstract val resourceManager: AndroidResourceManager

    private val vfsTracker: VfsModificationTracker by Delegates.lazy {
        VfsModificationTracker(project, resourceManager.getMainLayoutDirectory())
    }

    private val cachedSources: CachedValue<List<String>> by Delegates.lazy {
        cachedValue {
            Result.create(parse(), vfsTracker)
        }
    }

    private val cachedJetFiles: CachedValue<List<JetFile>> by Delegates.lazy {
        cachedValue {
            val psiManager = PsiManager.getInstance(project)
            val applicationPackage = resourceManager.androidModuleInfo?.applicationPackage

            val jetFiles = cachedSources.getValue().mapIndexed { (index, text) ->
                val virtualFile = LightVirtualFile(AndroidConst.SYNTHETIC_FILENAME + index + ".kt", text)
                val jetFile = psiManager.findFile(virtualFile) as JetFile
                if (applicationPackage != null) {
                    jetFile.putUserData(AndroidConst.ANDROID_USER_PACKAGE, applicationPackage)
                }
                jetFile
            }

            Result.create(jetFiles, cachedSources)
        }
    }

    protected val LOG: Logger = Logger.getInstance(javaClass)

    public fun parse(): List<String> {
        return resourceManager.getLayoutXmlFiles().map { file ->
            val widgets = parseSingleFile(file)
            if (widgets.isNotEmpty()) {
                val layoutPackage = file.genSyntheticPackageName()
                val stringWriter = KotlinStringWriter()

                stringWriter.writePackage(layoutPackage)
                stringWriter.writeAndroidImports()
                widgets.forEach { stringWriter.writeSyntheticActivityProperty(it) }

                val contents = stringWriter.toStringBuffer().toString()
                contents
            } else null
        }.filterNotNull()
    }

    public fun parseToPsi(): List<JetFile>? = cachedJetFiles.getValue()

    protected abstract fun parseSingleFile(file: PsiFile): Collection<AndroidWidget>

    private fun KotlinStringWriter.writeAndroidImports() {
        androidImports.forEach { writeImport(it) }
        writeEmptyLine()
    }

    private fun PsiFile.genSyntheticPackageName(): String {
        return AndroidConst.SYNTHETIC_PACKAGE + getName().substringBefore('.')
    }

    private fun KotlinStringWriter.writeSyntheticActivityProperty(widget: AndroidWidget) {
        val body = arrayListOf("return findViewById(0) as ${widget.className}")
        writeImmutableExtensionProperty(receiver = "Activity",
                                        name = widget.id,
                                        retType = widget.className,
                                        getterBody = body)
    }

    private fun <T> cachedValue(result: () -> CachedValueProvider.Result<T>): CachedValue<T> {
        return CachedValuesManager.getManager(project).createCachedValue(result, false)
    }

}

private class VfsModificationTracker(project: Project, resDirectory: VirtualFile?): SimpleModificationTracker() {
    {
        val connection = project.getMessageBus().connect();
        connection.subscribe(VirtualFileManager.VFS_CHANGES, BulkVirtualFileListenerAdapter(
                object : VirtualFileListener {
                    fun incModificationCountIfLayout(file: VirtualFile) {
                        if (resDirectory == null) {
                            incModificationCount()
                        } else {
                            val probablyLayoutDir = file.getParent()
                            val probablyResDir = file.getParent()?.getParent()
                            if (resDirectory == probablyResDir && probablyLayoutDir?.getName()?.startsWith("layout") ?: false) {
                                incModificationCount()
                            }
                        }
                    }

                    override fun contentsChanged(event: VirtualFileEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun propertyChanged(event: VirtualFilePropertyEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun fileCreated(event: VirtualFileEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun fileDeleted(event: VirtualFileEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun fileMoved(event: VirtualFileMoveEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun fileCopied(event: VirtualFileCopyEvent) {
                        incModificationCountIfLayout(event.getFile())
                    }

                    override fun beforePropertyChange(event: VirtualFilePropertyEvent) {}

                    override fun beforeContentsChange(event: VirtualFileEvent) {}

                    override fun beforeFileDeletion(event: VirtualFileEvent) {}

                    override fun beforeFileMovement(event: VirtualFileMoveEvent) {}
                }
        ))
    }
}

