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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.KotlinStringWriter
import org.jetbrains.kotlin.android.synthetic.escapeAndroidIdentifier
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.types.Flexibility

public class AndroidSyntheticFile(val name: String, val contents: String)

public abstract class SyntheticFileGenerator(protected val project: Project) {

    public class NoAndroidManifestFound : Exception("No android manifest file found in project root")

    public abstract val layoutXmlFileManager: AndroidLayoutXmlFileManager

    protected abstract fun supportV4(): Boolean

    public abstract fun getSyntheticFiles(): List<JetFile>

    protected fun generateSyntheticFiles(generateCommonFiles: Boolean = true, scope: GlobalSearchScope): List<AndroidSyntheticFile> {
        val commonFiles = if (generateCommonFiles) generateCommonFiles() else listOf()

        return layoutXmlFileManager.getLayoutXmlFiles().flatMap { entry ->
            val files = entry.getValue()
            val resources = extractLayoutResources(files, scope)

            val layoutName = entry.getKey()

            val mainLayoutFile = renderMainLayoutFile(layoutName, resources)
            val viewLayoutFile = renderViewLayoutFile(layoutName, resources)

            listOf(mainLayoutFile, viewLayoutFile)
        }.filterNotNull() + commonFiles
    }

    private fun generateCommonFiles(): List<AndroidSyntheticFile> {
        val renderSyntheticFile = renderSyntheticFile("clearCache") {
            writePackage(AndroidConst.SYNTHETIC_PACKAGE)
            writeAndroidImports()
            writeClearCacheFunction(AndroidConst.ACTIVITY_FQNAME)
            writeClearCacheFunction(AndroidConst.FRAGMENT_FQNAME)
            if (supportV4()) writeClearCacheFunction(AndroidConst.SUPPORT_FRAGMENT_FQNAME)
        }
        val clearCacheFile = renderSyntheticFile

        return listOf(
               clearCacheFile,
               FLEXIBLE_TYPE_FILE,
               FAKE_SUPPORT_V4_APP_FILE,
               FAKE_SUPPORT_V4_VIEW_FILE,
               FAKE_SUPPORT_V4_WIDGET_FILE)
    }

    protected abstract fun extractLayoutResources(files: List<PsiFile>, scope: GlobalSearchScope): List<AndroidResource>

    private fun renderMainLayoutFile(layoutName: String, resources: List<AndroidResource>): AndroidSyntheticFile {
        return renderLayoutFile(layoutName + AndroidConst.LAYOUT_POSTFIX,
                                escapeAndroidIdentifier(layoutName), resources) {
            val properties = it.mainProperties.toArrayList()
            if (supportV4()) properties.addAll(it.mainPropertiesForSupportV4)
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

    private fun KotlinStringWriter.writeSyntheticProperty(receiver: String, resource: AndroidResource, stubCall: String) {
        // extract startsWith() to fun
        val className = if (isFromSupportV4Package(receiver)) resource.supportClassName else resource.className
        val cast = if (resource.className != "View") " as? $className" else ""
        val body = arrayListOf("return $stubCall$cast")
        writeImmutableExtensionProperty(receiver,
                                        name = resource.id,
                                        retType = "$EXPLICIT_FLEXIBLE_CLASS_NAME<$className, $className?>",
                                        getterBody = body)
    }

    private fun KotlinStringWriter.writeClearCacheFunction(receiver: String) {
        writeText("public fun $receiver.${AndroidConst.CLEAR_FUNCTION_NAME}() {}\n")
    }

    protected fun <T> cachedValue(result: () -> Result<T>): CachedValue<T> {
        return CachedValuesManager.getManager(project).createCachedValue(result, false)
    }

    private fun isFromSupportV4Package(fqName: String): Boolean {
        return fqName.startsWith(AndroidConst.SUPPORT_V4_PACKAGE)
    }

    protected fun resolveFqClassNameForView(javaPsiFacade: JavaPsiFacade, scope: GlobalSearchScope, tag: String): String? {
        if (tag.contains('.')) {
            if (javaPsiFacade.findClass(tag, scope) == null) {
                return null
            }
            return tag
        }
        for (pkg in AndroidConst.FQNAME_RESOLVE_PACKAGES) {
            val fqName = "$pkg.$tag"
            if (javaPsiFacade.findClass(fqName, scope) != null) {
                return fqName
            }
        }
        return null
    }

    protected fun filterDuplicates(resources: List<AndroidResource>): List<AndroidResource> {
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

    protected fun generateSyntheticJetFiles(files: List<AndroidSyntheticFile>): List<JetFile> {
        val psiManager = PsiManager.getInstance(project)
        val applicationPackage = layoutXmlFileManager.androidModuleInfo?.applicationPackage

        return files.mapIndexed { index, syntheticFile ->
            val fileName = AndroidConst.SYNTHETIC_FILENAME_PREFIX + syntheticFile.name + ".kt"
            val virtualFile = LightVirtualFile(fileName, syntheticFile.contents)
            val jetFile = psiManager.findFile(virtualFile) as JetFile
            if (applicationPackage != null) {
                jetFile.putUserData(AndroidConst.ANDROID_USER_PACKAGE, applicationPackage)
            }
            jetFile
        }
    }

    companion object {
        private val EXPLICIT_FLEXIBLE_PACKAGE = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.packageFqName.asString()
        private val EXPLICIT_FLEXIBLE_CLASS_NAME = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.relativeClassName.asString()

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