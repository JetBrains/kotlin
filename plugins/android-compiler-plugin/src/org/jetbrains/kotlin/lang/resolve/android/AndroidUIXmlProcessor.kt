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
import com.intellij.psi.impl.*
import org.jetbrains.kotlin.types.Flexibility

public class AndroidSyntheticFile(val name: String, val contents: String)

public abstract class AndroidUIXmlProcessor(protected val project: Project) {

    public class NoAndroidManifestFound : Exception("No android manifest file found in project root")

    protected val LOG: Logger = Logger.getInstance(javaClass)

    public abstract val resourceManager: AndroidResourceManager

    protected abstract val cachedSources: CachedValue<List<AndroidSyntheticFile>>

    //MAKE CONSTANT (or abstract)
    var supportV4 = false

    private val cachedJetFiles: CachedValue<List<JetFile>> by Delegates.lazy {
        cachedValue {
            val psiManager = PsiManager.getInstance(project)
            val applicationPackage = resourceManager.androidModuleInfo?.applicationPackage

            val jetFiles = cachedSources.getValue().mapIndexed { index, syntheticFile ->
                val fileName = AndroidConst.SYNTHETIC_FILENAME_PREFIX + syntheticFile.name + ".kt"
                val virtualFile = LightVirtualFile(fileName, syntheticFile.contents)
                val jetFile = psiManager.findFile(virtualFile) as JetFile
                if (applicationPackage != null) {
                    jetFile.putUserData(AndroidConst.ANDROID_USER_PACKAGE, applicationPackage)
                }
                jetFile
            }

            Result.create(jetFiles, cachedSources)
        }
    }

    public fun parse(generateCommonFiles: Boolean = true): List<AndroidSyntheticFile> {
        val commonFiles = if (generateCommonFiles) {
            val clearCacheFile = renderSyntheticFile("clearCache") {
                writePackage(AndroidConst.SYNTHETIC_PACKAGE)
                writeAndroidImports()
                writeClearCacheFunction(AndroidConst.ACTIVITY_FQNAME)
                writeClearCacheFunction(AndroidConst.FRAGMENT_FQNAME)
                if (supportV4) writeClearCacheFunction(AndroidConst.SUPPORT_FRAGMENT_FQNAME)
            }

            listOf(clearCacheFile,
                   FLEXIBLE_TYPE_FILE,
                   FAKE_SUPPORT_V4_APP_FILE,
                   FAKE_SUPPORT_V4_VIEW_FILE,
                   FAKE_SUPPORT_V4_WIDGET_FILE)
        } else listOf()

        return resourceManager.getLayoutXmlFiles().flatMap { entry ->
            val files = entry.getValue()
            val resources = parseLayout(files)

            val layoutName = files[0].getName().substringBefore('.')

            val mainLayoutFile = renderMainLayoutFile(layoutName, resources)
            val viewLayoutFile = renderViewLayoutFile(layoutName, resources)

            listOf(mainLayoutFile, viewLayoutFile)
        }.filterNotNull() + commonFiles
    }

    public fun parseToPsi(): List<JetFile>? = cachedJetFiles.getValue()

    protected abstract fun parseLayout(files: List<PsiFile>): List<AndroidResource>

    private fun renderMainLayoutFile(layoutName: String, resources: List<AndroidResource>): AndroidSyntheticFile {
        return renderLayoutFile(layoutName + AndroidConst.LAYOUT_POSTFIX,
                                escapeAndroidIdentifier(layoutName), resources) {
            val properties = it.mainProperties.toArrayList()
            if (supportV4) properties.addAll(it.mainPropertiesForSupportV4)
            properties
        }
    }

    private fun renderViewLayoutFile(layoutName: String, resources: List<AndroidResource>): AndroidSyntheticFile {
        return renderLayoutFile(layoutName + AndroidConst.VIEW_LAYOUT_POSTFIX,
                                escapeAndroidIdentifier(layoutName) + ".view", resources) { it.viewProperties }
    }

    private fun renderLayoutFile(
            filename: String,
            packageSegment: String,
            resources: List<AndroidResource>,
            properties: (AndroidResource) -> List<Pair<String, String>>): AndroidSyntheticFile {
        return renderSyntheticFile(filename) {
            writePackage(AndroidConst.SYNTHETIC_PACKAGE + "." + packageSegment)
            writeAndroidImports()

            for (res in resources) {
                properties(res).forEach { property ->
                    writeSyntheticProperty(property.first, res, property.second)
                }
            }
        }
    }

    private fun renderSyntheticFile(filename: String, init: KotlinStringWriter.() -> Unit): AndroidSyntheticFile {
        val stringWriter = KotlinStringWriter()
        stringWriter.init()
        return AndroidSyntheticFile(filename, stringWriter.toStringBuffer().toString())
    }

    private fun KotlinStringWriter.writeAndroidImports() {
        ANDROID_IMPORTS.forEach { writeImport(it) }
        writeEmptyLine()
    }

    private fun KotlinStringWriter.writeSyntheticProperty(receiver: String, widget: AndroidResource, stubCall: String) {
        // extract startsWith() to fun
        val className = if (isFromSupportV4Package(receiver)) widget.supportClassName else widget.className
        val cast = if (widget.className != "View") " as? $className" else ""
        val body = arrayListOf("return $stubCall$cast")
        writeImmutableExtensionProperty(receiver,
                                        name = widget.id,
                                        retType = "$EXPLICIT_FLEXIBLE_CLASS_NAME<$className, $className?>",
                                        getterBody = body)
    }

    private fun KotlinStringWriter.writeClearCacheFunction(receiver: String) {
        writeText("public fun $receiver.${AndroidConst.CLEAR_FUNCTION_NAME}() {}\n")
    }

    protected fun <T> cachedValue(result: () -> CachedValueProvider.Result<T>): CachedValue<T> {
        return CachedValuesManager.getManager(project).createCachedValue(result, false)
    }

    private fun isFromSupportV4Package(fqName: String): Boolean {
        return fqName.startsWith(AndroidConst.SUPPORT_V4_PACKAGE)
    }

    protected fun removeDuplicates(resources: List<AndroidResource>): List<AndroidResource> {
        val resourceMap = linkedMapOf<String, AndroidResource>()
        val resourcesToExclude = hashSetOf<String>()

        for (res in resources) {
            if (resourceMap.contains(res.id)) {
                val existing = resourceMap[res.id]!!

                if (!res.sameClass(existing)) {
                    resourcesToExclude.add(res.id)
                } else if (res is AndroidWidget && existing.className != res.className && existing.className != "View") {
                    // Widgets with the same id but different types exist.
                    resourceMap.put(res.id, AndroidWidget(res.id, "View"))
                }
            }
            else resourceMap.put(res.id, res)
        }
        resourcesToExclude.forEach { resourceMap.remove(it) }
        return resourceMap.values().toList()
    }

    companion object {
        private val EXPLICIT_FLEXIBLE_PACKAGE = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName().asString()
        private val EXPLICIT_FLEXIBLE_CLASS_NAME = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName().asString()

        private val ANDROID_IMPORTS = listOf(
                "android.app.*",
                "android.view.*",
                "android.widget.*",
                "android.webkit.*",
                "android.inputmethodservice.*",
                "android.opengl.*",
                "android.appwidget.*",
                "android.support.v4.app.*",
                "android.support.v4.view.*",
                "android.support.v4.widget.*",
                Flexibility.FLEXIBLE_TYPE_CLASSIFIER.asSingleFqName().asString())

        private val FLEXIBLE_TYPE_FILE =
                AndroidSyntheticFile("ft", "package $EXPLICIT_FLEXIBLE_PACKAGE\n\nclass $EXPLICIT_FLEXIBLE_CLASS_NAME<L, U>")

        private val FAKE_SUPPORT_V4_WIDGET_FILE = AndroidSyntheticFile("supportv4_widget", "package android.support.v4.widget")

        private val FAKE_SUPPORT_V4_VIEW_FILE = AndroidSyntheticFile("supportv4_view", "package android.support.v4.view")

        private val FAKE_SUPPORT_V4_APP_FILE = AndroidSyntheticFile("supportv4_app", "package android.support.v4.app")
    }

}