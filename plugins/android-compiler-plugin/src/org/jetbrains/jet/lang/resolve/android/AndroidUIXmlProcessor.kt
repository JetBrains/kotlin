/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentLinkedQueue
import com.intellij.openapi.util.Key
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
import com.intellij.openapi.util.ModificationTracker

public abstract class AndroidUIXmlProcessor(protected val project: Project) {

    public class NoAndroidManifestFound : Exception("No android manifest file found in project root")

    private val androidImports = arrayListOf("android.app.Activity",
                                     "android.view.View",
                                     "android.widget.*")

    private val cachedSourceText = CachedValuesManager.getManager(project).createCachedValue {
        Result.create(parse(), ProjectRootModificationTracker.getInstance(project))
    }

    private val cachedJetFile = CachedValuesManager.getManager(project).createCachedValue {
        //TODO: dep checking .. cachedSourceText.getValue()
        val virtualFile = LightVirtualFile(AndroidConst.SYNTHETIC_FILENAME, parse()) //cachedSourceTest.getValue()
        val jetFile = PsiManager.getInstance(project).findFile(virtualFile) as JetFile

        val applicationPackage = resourceManager.androidModuleInfo?.applicationPackage
        if (applicationPackage != null) jetFile.putUserData(AndroidConst.ANDROID_USER_PACKAGE, applicationPackage)

        Result.create(jetFile, cachedSourceText)
    }

    public abstract val resourceManager: AndroidResourceManager

    protected val LOG: Logger = Logger.getInstance(javaClass)

    public fun parse(): String {
        val buffer = writeImports(KotlinStringWriter()).output()
        for (file in resourceManager.getLayoutXmlFiles()) {
            buffer.append(parseSingleFile(file))
        }
        return buffer.toString()
    }

    public fun parseToPsi(): JetFile? = cachedJetFile.getValue()

    protected abstract fun parseSingleFile(file: PsiFile): String

    private fun writeImports(kw: KotlinStringWriter): KotlinWriter {
        val applicationPackage = resourceManager.androidModuleInfo?.applicationPackage
        if (applicationPackage != null) kw.writePackage(applicationPackage)

        for (elem in androidImports) {
            kw.writeImport(elem)
        }

        kw.writeEmptyLine()
        return kw
    }

    protected fun produceKotlinProperties(kw: KotlinStringWriter, ids: Collection<AndroidWidget>): StringBuffer {
        for (id in ids) {
            val body = arrayListOf("return findViewById(0) as ${id.className}")
            kw.writeImmutableExtensionProperty(receiver = "Activity",
                                               name = id.id,
                                               retType = id.className,
                                               getterBody = body)
        }
        return kw.output()
    }
}
