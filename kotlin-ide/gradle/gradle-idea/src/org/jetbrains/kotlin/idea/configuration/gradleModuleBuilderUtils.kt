/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCoreUtil
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import java.io.File

internal var Module.gradleModuleBuilder: AbstractExternalModuleBuilder<*>? by UserDataProperty(Key.create("GRADLE_MODULE_BUILDER"))
private var Module.settingsScriptBuilder: SettingsScriptBuilder<out PsiFile>? by UserDataProperty(Key.create("SETTINGS_SCRIPT_BUILDER"))

internal fun findSettingsGradleFile(module: Module): VirtualFile? {
    val contentEntryPath = module.gradleModuleBuilder?.contentEntryPath ?: return null
    if (contentEntryPath.isEmpty()) return null
    val contentRootDir = File(contentEntryPath)
    val modelContentRootDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contentRootDir) ?: return null
    return modelContentRootDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
        ?: modelContentRootDir.findChild(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME)
        ?: module.project.baseDir.findChild(GradleConstants.SETTINGS_FILE_NAME)
        ?: module.project.baseDir.findChild(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME)
}

abstract class SettingsScriptBuilder<T: PsiFile>(val scriptFile: T) {
    private val builder = StringBuilder(scriptFile.text)

    private fun findBlockBody(blockName: String, startFrom: Int = 0): Int {
        val blockOffset = builder.indexOf(blockName, startFrom)
        if (blockOffset < 0) return -1
        return builder.indexOf('{', blockOffset + 1) + 1
    }

    private fun getOrPrependTopLevelBlockBody(blockName: String): Int {
        val blockBody = findBlockBody(blockName)
        if (blockBody >= 0) return blockBody
        builder.insert(0, "$blockName {}\n")
        return findBlockBody(blockName)
    }

    private fun getOrAppendInnerBlockBody(blockName: String, offset: Int): Int {
        val repositoriesBody = findBlockBody(blockName, offset)
        if (repositoriesBody >= 0) return repositoriesBody
        builder.insert(offset, "\n$blockName {}\n")
        return findBlockBody(blockName, offset)
    }

    private fun appendExpressionToBlockIfAbsent(expression: String, offset: Int) {
        var braceCount = 1
        var blockEnd = offset
        for (i in offset..builder.lastIndex) {
            when (builder[i]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            if (braceCount == 0) {
                blockEnd = i
                break
            }
        }
        if (!builder.substring(offset, blockEnd).contains(expression.trim())) {
            builder.insert(blockEnd, "\n$expression\n")
        }
    }

    private fun getOrCreatePluginManagementBody() = getOrPrependTopLevelBlockBody("pluginManagement")

    protected fun addPluginRepositoryExpression(expression: String) {
        val repositoriesBody = getOrAppendInnerBlockBody("repositories", getOrCreatePluginManagementBody())
        appendExpressionToBlockIfAbsent(expression, repositoriesBody)
    }

    fun addMavenCentralPluginRepository() {
        addPluginRepositoryExpression("mavenCentral()")
    }

    abstract fun addPluginRepository(repository: RepositoryDescription)

    fun addResolutionStrategy(pluginId: String) {
        val resolutionStrategyBody = getOrAppendInnerBlockBody("resolutionStrategy", getOrCreatePluginManagementBody())
        val eachPluginBody = getOrAppendInnerBlockBody("eachPlugin", resolutionStrategyBody)
        appendExpressionToBlockIfAbsent(
            """
                if (requested.id.id == "$pluginId") {
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
                }
            """.trimIndent(),
            eachPluginBody
        )
    }

    fun addIncludedModules(modules: List<String>) {
        builder.append(modules.joinToString(prefix = "include ", postfix = "\n") { "'$it'" })
    }

    fun build() = builder.toString()

    abstract fun buildPsiFile(project: Project): T
}


class GroovySettingsScriptBuilder(scriptFile: GroovyFile): SettingsScriptBuilder<GroovyFile>(scriptFile) {
    override fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toGroovyRepositorySnippet())
    }

    override fun buildPsiFile(project: Project): GroovyFile {
        return GroovyPsiElementFactory
            .getInstance(project)
            .createGroovyFile(build(), false, null)
    }
}
class KotlinSettingsScriptBuilder(scriptFile: KtFile): SettingsScriptBuilder<KtFile>(scriptFile) {
    override fun addPluginRepository(repository: RepositoryDescription) {
        addPluginRepositoryExpression(repository.toKotlinRepositorySnippet())
    }

    override fun buildPsiFile(project: Project): KtFile {
        return KtPsiFactory(project).createFile(build())
    }
}

// Circumvent write actions and modify the file directly
// TODO: Get rid of this hack when IDEA API allows manipulation of settings script similarly to the main script itself
internal fun updateSettingsScript(module: Module, updater: (SettingsScriptBuilder<out PsiFile>) -> Unit) {
    val storedSettingsBuilder = module.settingsScriptBuilder
    val settingsBuilder =
        storedSettingsBuilder
            ?: (findSettingsGradleFile(module)?.toPsiFile(module.project))?.let {
                if (it is KtFile) {
                    KotlinSettingsScriptBuilder(it)
                } else if (it is GroovyFile) {
                    GroovySettingsScriptBuilder(it)
                } else {
                    null
                }
            }
            ?: return
    if (storedSettingsBuilder == null) {
        module.settingsScriptBuilder = settingsBuilder
    }
    updater(settingsBuilder)
}

internal fun flushSettingsGradleCopy(module: Module) {
    try {
        val settingsFile = findSettingsGradleFile(module)
        val settingsScriptBuilder = module.settingsScriptBuilder
        if (settingsScriptBuilder != null && settingsFile != null) {
            // The module.project is not opened yet.
            // Due to optimization in ASTDelegatePsiElement.getManager() and relevant ones,
            // we have to take theOnlyOpenProject() for manipulations with tmp file
            // (otherwise file will have one parent project and its elements will have other parent project,
            // and we will get KT-29333 problem).
            // TODO: get rid of file manipulations until project is opened
            val project = ProjectCoreUtil.theOnlyOpenProject() ?: module.project
            val tmpFile = settingsScriptBuilder.buildPsiFile(project)
            CodeStyleManager.getInstance(project).reformat(tmpFile)
            VfsUtil.saveText(settingsFile, tmpFile.text)
        }
    } finally {
        module.gradleModuleBuilder = null
        module.settingsScriptBuilder = null
    }
}

class KotlinGradleFrameworkSupportInModuleConfigurable(
    private val model: FrameworkSupportModel,
    private val supportProvider: GradleFrameworkSupportProvider
) : FrameworkSupportInModuleConfigurable() {
    override fun createComponent() = supportProvider.createComponent()

    override fun addSupport(
        module: Module,
        rootModel: ModifiableRootModel,
        modifiableModelsProvider: ModifiableModelsProvider
    ) {
        val buildScriptData = GradleModuleBuilder.getBuildScriptData(module)
        if (buildScriptData != null) {
            val builder = model.moduleBuilder
            val projectId = (builder as? GradleModuleBuilder)?.projectId ?: ProjectId(null, module.name, null)
            try {
                module.gradleModuleBuilder = builder as? AbstractExternalModuleBuilder<*>
                supportProvider.addSupport(projectId, module, rootModel, modifiableModelsProvider, buildScriptData)
            } finally {
                flushSettingsGradleCopy(module)
            }
        }
    }
}