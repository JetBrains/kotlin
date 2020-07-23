/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.properties.ModuleConfiguratorProperty
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectPath
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTextDescriptor
import java.nio.file.Path

object MppModuleConfigurator : ModuleConfigurator,
    ModuleConfiguratorWithSettings,
    ModuleConfiguratorSettings(),
    ModuleConfiguratorProperties,
    ModuleConfiguratorWithProperties {

    override val moduleKind = ModuleKind.multiplatform

    @NonNls
    override val suggestedModuleName = "shared"

    @NonNls
    override val id = "multiplatform"
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.mpp")
    override val canContainSubModules = true

    override fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.multiplatform,
            version = configurationData.kotlinVersion
        )

    val mppFiles by listProperty<MppFile>()

    override fun getConfiguratorProperties(): List<ModuleConfiguratorProperty<*>> =
        listOf(mppFiles)

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path,
    ): TaskResult<Unit> = inContextOfModuleConfigurator(module) {
        val mppFiles = mppFiles.reference.propertyValue.distinctBy { it.fileNameWithPackage } // todo merge files with the same name here
        mppFiles.mapSequenceIgnore { file ->
            module.subModules.mapSequenceIgnore mapTargets@{ target ->
                val moduleSubType = //TODO handle for non-simple target configurator
                    target.configurator.safeAs<SimpleTargetConfigurator>()?.moduleSubType ?: return@mapTargets UNIT_SUCCESS
                val fileTemplate = FileTemplate(
                    FileTextDescriptor(file.printForModuleSubType(moduleSubType), pathForFileInTarget(modulePath, module, file, target)),
                    projectPath,
                )
                TemplatesPlugin.fileTemplatesToRender.addValues(fileTemplate)
            }
        }
    }

    private fun pathForFileInTarget(mppModulePath: Path, mppModule: Module, file: MppFile, target: Module) =
        mppModulePath /
                Defaults.SRC_DIR /
                "${target.name}Main" /
                mppModule.configurator.kotlinDirectoryName /
                file.javaPackage?.asPath() /
                file.filename
}


@DslMarker
annotation class ExpectFileDSL

data class MppFile(
    @NonNls val filename: String,
    @NonNls val javaPackage: JavaPackage? = null,
    val declarations: List<MppDeclaration>
) {

    val fileNameWithPackage
        get() = javaPackage?.let { "${it.asCodePackage()}." }.orEmpty() + filename

    fun printForModuleSubType(moduleSubType: ModuleSubType): String = buildString {
        javaPackage?.let { appendLine("package ${it.asCodePackage()}"); appendLine() }
        if (moduleSubType == ModuleSubType.common) {
            declarations.joinTo(this, separator = "\n\n") { it.printExpect() }
        } else {
            printImports(declarations.flatMap { it.importsFor(moduleSubType) })
            declarations.joinTo(this, separator = "\n\n") { it.printActualFor(moduleSubType) }
        }
    }.trim()

    private fun StringBuilder.printImports(imports: List<String>) {
        if (imports.isEmpty()) return
        imports.distinct().joinTo(this, separator = "\n", prefix = "\n", postfix = "\n\n") { import ->
            "import $import"
        }
    }

    @ExpectFileDSL
    class Builder(private val filename: String) {
        var `package`: String? = null
        private val declarations = mutableListOf<MppDeclaration>()

        fun function(signature: String, init: MppFunction.Builder.() -> Unit = {}) {
            declarations += MppFunction.Builder(signature).apply(init).build()
        }

        fun `class`(name: String, init: MppClass.Builder.() -> Unit = {}) {
            declarations += MppClass.Builder(name).apply(init).build()
        }

        fun build() = MppFile(filename, `package`?.let(::JavaPackage), declarations)
    }
}

@ExpectFileDSL
class MppFilesListBuilder {
    private val expectFiles = mutableListOf<MppFile>()

    fun mppfile(filename: String, init: MppFile.Builder.() -> Unit) {
        expectFiles += MppFile.Builder(filename).apply(init).build()
    }

    fun build(): List<MppFile> = expectFiles
}

fun mppFiles(init: MppFilesListBuilder.() -> Unit): List<MppFile> =
    MppFilesListBuilder().apply(init).build()

sealed class MppDeclaration {
    @Suppress("SpellCheckingInspection")
    abstract val actuals: Actuals

    abstract fun printExpect(): String

    abstract fun printActualFor(moduleSubType: ModuleSubType): String

    fun importsFor(moduleSubType: ModuleSubType): List<String> = actuals.getBodyFor(moduleSubType).imports

    @ExpectFileDSL
    abstract class Builder {
        private var defaultActualDeclarationBody = DefaultActualDeclarationBody(text = "", imports = emptyList())

        @Suppress("SpellCheckingInspection")
        private val actuals = mutableMapOf<ModuleSubType, ActualDeclarationBody>()

        fun default(defaultText: String, init: DefaultActualDeclarationBody.Builder.() -> Unit = {}) {
            defaultActualDeclarationBody = DefaultActualDeclarationBody.Builder(defaultText).also(init).build()
        }

        fun actualFor(vararg moduleSubTypes: ModuleSubType, actualBody: String, init: ActualDeclarationBody.Builder.() -> Unit = {}) {
            moduleSubTypes.forEach { moduleSubType ->
                actuals[moduleSubType] = ActualDeclarationBody.Builder(actualBody).apply(init).build()
            }
        }

        protected fun buildActuals() = Actuals(defaultActualDeclarationBody, actuals)
    }
}

data class MppFunction(
    val signature: String,
    @Suppress("SpellCheckingInspection")
    override val actuals: Actuals,
) : MppDeclaration() {
    override fun printExpect(): String =
        "expect fun $signature"

    override fun printActualFor(moduleSubType: ModuleSubType): String {
        val body = actuals.getBodyFor(moduleSubType)
        return buildString {
            appendLine("actual fun $signature {")
            appendLine(body.text.indented(4))
            append("}")
        }
    }

    @ExpectFileDSL
    class Builder(private val signature: String) : MppDeclaration.Builder() {
        fun build(): MppFunction = MppFunction(
            signature,
            buildActuals(),
        )
    }
}

data class MppClass(
    val name: String,
    val expectBody: String?,
    @Suppress("SpellCheckingInspection")
    override val actuals: Actuals,
) : MppDeclaration() {
    override fun printExpect(): String = buildString {
        append("expect class $name()")
        expectBody?.let { body ->
            appendLine(" {")
            appendLine(body.indented(4))
            append("}")
        }
    }

    override fun printActualFor(moduleSubType: ModuleSubType): String {
        val body = actuals.getBodyFor(moduleSubType)
        return buildString {
            appendLine("actual class $name actual constructor() {")
            appendLine(body.text.indented(4))
            append("}")
        }
    }

    @ExpectFileDSL
    class Builder(private val name: String) : MppDeclaration.Builder() {
        var expectBody: String? = null

        fun build(): MppClass = MppClass(
            name,
            expectBody,
            buildActuals(),
        )
    }
}


private fun String.indented(indentValue: Int): String {
    val indent = " ".repeat(indentValue)
    return split('\n').joinToString(separator = "\n") { line -> indent + line.trim() }
}

@Suppress("SpellCheckingInspection")
data class Actuals(
    val defaultActualDeclarationBody: DefaultActualDeclarationBody,
    val actuals: Map<ModuleSubType, ActualDeclarationBody>,
) {
    fun getBodyFor(moduleSubType: ModuleSubType): DeclarationBody =
        actuals[moduleSubType] ?: defaultActualDeclarationBody
}

sealed class DeclarationBody {
    abstract val text: String
    abstract val imports: List<String>

    abstract class Builder {
        protected val imports = mutableListOf<String>()

        fun import(import: String) {
            imports += import
        }
    }
}

data class ActualDeclarationBody(override val text: String, override val imports: List<String>) : DeclarationBody() {
    class Builder(private val text: String) : DeclarationBody.Builder() {
        fun build() = ActualDeclarationBody(text, imports)
    }
}

data class DefaultActualDeclarationBody(override val text: String, override val imports: List<String>) : DeclarationBody() {
    class Builder(private val text: String) : DeclarationBody.Builder() {
        fun build() = DefaultActualDeclarationBody(text, imports)
    }
}