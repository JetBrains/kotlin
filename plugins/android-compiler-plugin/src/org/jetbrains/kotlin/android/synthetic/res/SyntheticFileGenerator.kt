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
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.KotlinStringWriter
import org.jetbrains.kotlin.android.synthetic.escapeAndroidIdentifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.Flexibility

public class AndroidSyntheticFile(val name: String, val contents: String) {
    companion object {
        fun create(name: String, vararg contents: String) = AndroidSyntheticFile(name, contents.joinToString("\n\n"))
    }
}

public abstract class SyntheticFileGenerator(protected val project: Project) {

    public class NoAndroidManifestFound : Exception("No android manifest file found in project root")

    public abstract val layoutXmlFileManager: AndroidLayoutXmlFileManager

    public abstract fun getSyntheticFiles(): List<KtFile>

    protected open fun generateSyntheticFiles(generateCommonFiles: Boolean, supportV4: Boolean): List<AndroidSyntheticFile> {
        val commonFiles = if (generateCommonFiles) generateCommonFiles(supportV4) else listOf()

        return layoutXmlFileManager.getLayoutXmlFiles().flatMap { variantData ->
            variantData.flatMap { entry ->
                val files = entry.value
                val resources = extractLayoutResources(files)

                val layoutName = entry.key

                arrayListOf<AndroidSyntheticFile>().apply {
                    this += renderMainLayoutFiles(variantData.variant, layoutName, resources, supportV4)
                    this += renderViewLayoutFiles(variantData.variant, layoutName, resources)
                }
            }
        }.filterNotNull() + commonFiles
    }

    private fun generateCommonFiles(supportV4: Boolean): List<AndroidSyntheticFile> {
        val renderSyntheticFile = renderSyntheticFile("clearCache") {
            writePackage(AndroidConst.SYNTHETIC_PACKAGE)
            writeAndroidImports()
            writeClearCacheFunction(AndroidConst.ACTIVITY_FQNAME)
            writeClearCacheFunction(AndroidConst.FRAGMENT_FQNAME)
            if (supportV4) {
                writeClearCacheFunction(AndroidConst.SUPPORT_FRAGMENT_FQNAME)
            }
        }
        val clearCacheFile = renderSyntheticFile

        return listOf(clearCacheFile, TOOLS_FILE)
    }

    protected abstract fun extractLayoutResources(files: List<PsiFile>): List<AndroidResource>

    protected abstract fun checkIfClassExist(fqName: String): Boolean

    private fun renderMainLayoutFiles(
            variant: AndroidVariant,
            layoutName: String,
            resources: List<AndroidResource>,
            supportV4: Boolean
    ): List<AndroidSyntheticFile> {
        return renderLayoutFile(variant, layoutName + AndroidConst.LAYOUT_POSTFIX, escapeAndroidIdentifier(layoutName), resources) {
            val properties = it.mainProperties.toArrayList()
            if (supportV4) properties.addAll(it.mainPropertiesForSupportV4)
            properties
        }
    }

    private fun renderViewLayoutFiles(
            variant: AndroidVariant,
            layoutName: String,
            resources: List<AndroidResource>
    ): List<AndroidSyntheticFile> {
        return renderLayoutFile(variant, layoutName + AndroidConst.VIEW_LAYOUT_POSTFIX,
                                escapeAndroidIdentifier(layoutName) + ".view", resources) { it.viewProperties }
    }

    private fun renderLayoutFile(
            variant: AndroidVariant,
            filename: String,
            layoutName: String,
            resources: List<AndroidResource>,
            properties: (AndroidResource) -> List<Pair<String, String>>): List<AndroidSyntheticFile> {
        fun render(defaultVariant: Boolean = false): AndroidSyntheticFile {
            val fullFilename = (if (defaultVariant) "" else "${variant.name}_") + filename
            return renderSyntheticFile(fullFilename) {
                val packageName = if (defaultVariant)
                    AndroidConst.SYNTHETIC_PACKAGE + "." + layoutName
                else
                    AndroidConst.SYNTHETIC_PACKAGE + '.' + variant.name + '.' + layoutName

                writePackage(packageName)
                writeAndroidImports()

                for (res in resources) {
                    properties(res).forEach { property ->
                        if (defaultVariant) {
                            val deprecatedText = "Use the property from the 'main' variant instead"
                            val import = AndroidConst.SYNTHETIC_PACKAGE + '.' + variant.name + '.' + layoutName + '.' + res.id
                            writeText("@Deprecated(\"$deprecatedText\", ReplaceWith(\"${res.id}\", \"$import\"))")
                        }
                        writeSyntheticProperty(property.first, res, property.second)
                    }
                }
            }
        }

        return if (variant.isMainVariant) listOf(render(), render(true)) else listOf(render())
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
        val className = if (isFromSupportV4Package(receiver)) resource.supportClassName else resource.className
        val cast = if (resource.className != AndroidConst.VIEW_FQNAME) " as? $className" else ""
        val body = arrayListOf("return $stubCall$cast")

        // Annotation on wrong widget type
        if (resource is AndroidWidget && resource.invalidType != null) {
            writeText("@${INVALID_WIDGET_TYPE_ANNOTATION_FQNAME.asString()}(\"${resource.invalidType}\")")
        }

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

    protected fun resolveFqClassNameForView(tag: String): String? {
        if (tag.contains('.')) {
            if (!checkIfClassExist(tag)) {
                return null
            }
            return tag
        }
        for (pkg in AndroidConst.FQNAME_RESOLVE_PACKAGES) {
            val fqName = "$pkg.$tag"
            if (checkIfClassExist(fqName)) {
                return fqName
            }
        }
        return null
    }

    protected fun parseAndroidResource(id: String, tag: String, fqNameResolver: (String) -> String?): AndroidResource {
        return when (tag) {
            "fragment" -> AndroidFragment(id)
            "include" -> AndroidWidget(id, AndroidConst.VIEW_FQNAME)
            else -> parseAndroidWidget(id, tag, fqNameResolver)
        }
    }

    protected abstract fun parseAndroidWidget(id: String, tag: String, fqNameResolver: (String) -> String?): AndroidResource

    protected fun supportV4Available(): Boolean = checkIfClassExist(AndroidConst.SUPPORT_FRAGMENT_FQNAME)

    protected fun filterDuplicates(resources: List<AndroidResource>): List<AndroidResource> {
        val resourceMap = linkedMapOf<String, AndroidResource>()
        val resourcesToExclude = hashSetOf<String>()

        for (res in resources) {
            if (resourceMap.contains(res.id)) {
                val existing = resourceMap[res.id]!!

                if (!res.sameClass(existing)) {
                    resourcesToExclude.add(res.id)
                } else if (res is AndroidWidget && existing.className != res.className && existing.className != AndroidConst.VIEW_FQNAME) {
                    // Widgets with the same id but different types exist.
                    resourceMap.put(res.id, AndroidWidget(res.id, AndroidConst.VIEW_FQNAME))
                }
            }
            else resourceMap.put(res.id, res)
        }
        resourcesToExclude.forEach { resourceMap.remove(it) }
        return resourceMap.values().toList()
    }

    protected fun generateSyntheticJetFiles(files: List<AndroidSyntheticFile>): List<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val applicationPackage = layoutXmlFileManager.androidModule?.applicationPackage

        return files.mapIndexed { index, syntheticFile ->
            val fileName = AndroidConst.SYNTHETIC_FILENAME_PREFIX + syntheticFile.name + ".kt"
            val virtualFile = LightVirtualFile(fileName, syntheticFile.contents)
            val jetFile = psiManager.findFile(virtualFile) as KtFile
            if (applicationPackage != null) {
                jetFile.putUserData(AndroidConst.ANDROID_USER_PACKAGE, applicationPackage)
            }
            jetFile
        }
    }

    public companion object {
        private val EXPLICIT_FLEXIBLE_PACKAGE = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.packageFqName.asString()
        private val EXPLICIT_FLEXIBLE_CLASS_NAME = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.relativeClassName.asString()

        private val ANDROID_IMPORTS = listOf(Flexibility.FLEXIBLE_TYPE_CLASSIFIER.asSingleFqName().asString())

        private val INVALID_WIDGET_TYPE_ANNOTATION = "InvalidWidgetType"
        public val INVALID_WIDGET_TYPE_ANNOTATION_TYPE_PARAMETER: String = "type"
        public val INVALID_WIDGET_TYPE_ANNOTATION_FQNAME: FqName = FqName("$EXPLICIT_FLEXIBLE_PACKAGE.$INVALID_WIDGET_TYPE_ANNOTATION")

        private val TOOLS_FILE = AndroidSyntheticFile.create("tools",
                "package $EXPLICIT_FLEXIBLE_PACKAGE",
                "class $EXPLICIT_FLEXIBLE_CLASS_NAME<L, U>",
                "annotation class $INVALID_WIDGET_TYPE_ANNOTATION(public val $INVALID_WIDGET_TYPE_ANNOTATION_TYPE_PARAMETER: String)")
    }

}