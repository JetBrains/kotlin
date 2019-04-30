/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.scripting.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

internal class ScriptLightVirtualFile(name: String, private val _path: String?, text: String) :
    LightVirtualFile(name, KotlinLanguage.INSTANCE, StringUtil.convertLineSeparators(text)) {

    init {
        charset = CharsetToolkit.UTF8_CHARSET
    }

    override fun getPath(): String = _path ?: super.getPath()
    override fun getCanonicalPath(): String? = path
}

internal fun makeCompiledModule(generationState: GenerationState) =
    KJvmCompiledModuleInMemory(
        generationState.factory.asList()
            .associateTo(sortedMapOf<String, ByteArray>()) { it.relativePath to it.asByteArray() }
    )

internal fun SourceCode.scriptFileName(
    mainScript: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): String =
    when {
        name != null -> name!!
        mainScript == this -> "script.${scriptCompilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
        else -> throw Exception("Unexpected script without name: $this")
    }

internal inline fun <T> withMessageCollectorAndDisposable(
    messageCollector: ScriptDiagnosticsMessageCollector = ScriptDiagnosticsMessageCollector(),
    disposable: Disposable = Disposer.newDisposable(),
    disposeOnSuccess: Boolean = true,
    locationId: String? = null,
    body: (ScriptDiagnosticsMessageCollector, Disposable) -> ResultWithDiagnostics<T>
): ResultWithDiagnostics<T> {
    var failed = false
    return try {
        setIdeaIoUseFallback()
        body(messageCollector, disposable).also {
            failed = it is ResultWithDiagnostics.Failure
        }
    } catch (ex: Throwable) {
        failed = true
        failure(messageCollector, ex.asDiagnostics(path = locationId))
    } finally {
        if (disposeOnSuccess || failed) {
            Disposer.dispose(disposable)
        }
    }
}

internal inline fun <T> withMessageCollector(
    script: SourceCode,
    messageCollector: ScriptDiagnosticsMessageCollector = ScriptDiagnosticsMessageCollector(),
    body: (ScriptDiagnosticsMessageCollector) -> ResultWithDiagnostics<T>
): ResultWithDiagnostics<T> =
    try {
        body(messageCollector)
    } catch (ex: Throwable) {
        failure(messageCollector, ex.asDiagnostics(path = script.locationId))
    }

internal fun getScriptKtFile(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    project: Project,
    messageCollector: ScriptDiagnosticsMessageCollector
): ResultWithDiagnostics<KtFile> {
    val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val scriptText = getMergedScriptText(script, scriptCompilationConfiguration)
    val virtualFile = ScriptLightVirtualFile(
        script.scriptFileName(script, scriptCompilationConfiguration),
        (script as? FileScriptSource)?.file?.path,
        scriptText
    )
    val ktFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
    return when {
        ktFile == null -> failure(script, messageCollector, "Unable to make PSI file from script")
        ktFile.declarations.firstIsInstanceOrNull<KtScript>() == null -> failure(script, messageCollector, "Not a script file")
        else -> ktFile.asSuccess()
    }
}

internal fun makeCompiledScript(
    generationState: GenerationState,
    script: SourceCode,
    ktFile: KtFile,
    sourceDependencies: List<ScriptsCompilationDependencies.SourceDependencies>,
    getScriptConfiguration: (KtFile) -> ScriptCompilationConfiguration
): KJvmCompiledScript<Any> {
    val scriptDependenciesStack = ArrayDeque<KtScript>()
    val ktScript = ktFile.declarations.firstIsInstanceOrNull<KtScript>()
        ?: throw IllegalStateException("Expecting script file: KtScript is not found in ${ktFile.name}")

    fun makeOtherScripts(script: KtScript): List<KJvmCompiledScript<*>> {

        // TODO: ensure that it is caught earlier (as well) since it would be more economical
        if (scriptDependenciesStack.contains(script))
            throw IllegalArgumentException("Unable to handle recursive script dependencies")
        scriptDependenciesStack.push(script)

        val containingKtFile = script.containingKtFile
        val otherScripts: List<KJvmCompiledScript<*>> =
            sourceDependencies.find { it.scriptFile == containingKtFile }?.sourceDependencies?.mapNotNull { sourceFile ->
                sourceFile.declarations.firstIsInstanceOrNull<KtScript>()?.let {
                    KJvmCompiledScript<Any>(
                        containingKtFile.virtualFile?.path,
                        getScriptConfiguration(sourceFile),
                        it.fqName.asString(),
                        null,
                        makeOtherScripts(it),
                        null
                    )
                }
            } ?: emptyList()

        scriptDependenciesStack.pop()
        return otherScripts
    }

    val module = makeCompiledModule(generationState)

    val resultField = with(generationState.replSpecific) {
        // TODO: pass it in the configuration instead
        if (!hasResult || resultType == null || scriptResultFieldName == null) null
        else scriptResultFieldName!! to KotlinType(DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(resultType!!))
    }

    return KJvmCompiledScript(
        script.locationId,
        getScriptConfiguration(ktScript.containingKtFile),
        ktScript.fqName.asString(),
        resultField,
        makeOtherScripts(ktScript),
        module
    )
}

