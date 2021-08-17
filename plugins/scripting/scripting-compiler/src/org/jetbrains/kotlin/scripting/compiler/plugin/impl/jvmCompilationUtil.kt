/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.scripting.compiler.plugin.dependencies.ScriptsCompilationDependencies
import org.jetbrains.kotlin.scripting.resolve.ScriptLightVirtualFile
import org.jetbrains.kotlin.scripting.scriptFileName
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.Serializable
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.getMergedScriptText
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

internal fun makeCompiledModule(generationState: GenerationState) =
    KJvmCompiledModuleInMemoryImpl(
        generationState.factory.asList()
            .associateTo(sortedMapOf<String, ByteArray>()) { it.relativePath to it.asByteArray() }
    )

inline fun <T> withMessageCollectorAndDisposable(
    script: SourceCode? = null,
    parentMessageCollector: MessageCollector? = null,
    disposable: Disposable = Disposer.newDisposable(),
    disposeOnSuccess: Boolean = true,
    body: (ScriptDiagnosticsMessageCollector, Disposable) -> ResultWithDiagnostics<T>
): ResultWithDiagnostics<T> {
    var failed = false
    val messageCollector = ScriptDiagnosticsMessageCollector(parentMessageCollector)
    return try {
        setIdeaIoUseFallback()
        body(messageCollector, disposable).also {
            failed = it is ResultWithDiagnostics.Failure
        }
    } catch (ex: Throwable) {
        failed = true
        failure(messageCollector, ex.asDiagnostics(path = script?.locationId))
    } finally {
        if (disposeOnSuccess || failed) {
            Disposer.dispose(disposable)
        }
    }
}

inline fun <T> withMessageCollector(
    script: SourceCode? = null,
    parentMessageCollector: MessageCollector? = null,
    body: (ScriptDiagnosticsMessageCollector) -> ResultWithDiagnostics<T>
): ResultWithDiagnostics<T> {
    val messageCollector = ScriptDiagnosticsMessageCollector(parentMessageCollector)
    return try {
        body(messageCollector)
    } catch (ex: Throwable) {
        failure(messageCollector, ex.asDiagnostics(path = script?.locationId))
    }
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
        (script as? FileBasedScriptSource)?.file?.path, // TODO: should be absolute path here
        scriptText
    )
    val ktFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
    return when {
        ktFile == null -> failure(
            script,
            messageCollector,
            "Unable to make PSI file from script"
        )
        ktFile.declarations.firstIsInstanceOrNull<KtScript>() == null -> failure(
            script,
            messageCollector,
            "Not a script file"
        )
        else -> ktFile.asSuccess()
    }
}

class SourceCodeImpl(file: KtFile) : SourceCode, Serializable {
    override val text: String = file.text
    override val name: String? = file.name
    override val locationId: String? = file.virtualFilePath

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal fun makeCompiledScript(
    generationState: GenerationState,
    script: SourceCode,
    ktFile: KtFile,
    sourceDependencies: List<ScriptsCompilationDependencies.SourceDependencies>,
    getScriptConfiguration: (KtFile) -> ScriptCompilationConfiguration
): ResultWithDiagnostics<KJvmCompiledScript> {
    val scriptDependenciesStack = ArrayDeque<KtScript>()
    val ktScript = ktFile.declarations.firstIsInstanceOrNull<KtScript>()
        ?: throw IllegalStateException("Expecting script file: KtScript is not found in ${ktFile.name}")

    fun makeOtherScripts(script: KtScript): ResultWithDiagnostics<List<KJvmCompiledScript>> {

        // TODO: ensure that it is caught earlier (as well) since it would be more economical
        if (scriptDependenciesStack.contains(script))
            return ResultWithDiagnostics.Failure(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    "Unable to handle recursive script dependencies",
                    sourcePath = script.containingFile.virtualFile?.path
                )
            )
        scriptDependenciesStack.push(script)

        val containingKtFile = script.containingKtFile
        val otherScripts =
            sourceDependencies.find { it.scriptFile == containingKtFile }?.sourceDependencies?.valueOrThrow()
                ?.mapNotNullSuccess { sourceFile ->
                    sourceFile.declarations.firstIsInstanceOrNull<KtScript>()?.let { ktScript ->
                        makeOtherScripts(ktScript).onSuccess { otherScripts ->
                            KJvmCompiledScript(
                                containingKtFile.virtualFilePath,
                                getScriptConfiguration(sourceFile),
                                ktScript.fqName.asString(),
                                null,
                                otherScripts,
                                null
                            ).asSuccess()
                        }
                    } ?: null.asSuccess()
                } ?: emptyList<KJvmCompiledScript>().asSuccess()

        scriptDependenciesStack.pop()
        return otherScripts
    }

    val module = makeCompiledModule(generationState)

    val resultField = with(generationState.scriptSpecific) {
        if (resultFieldName == null) null
        else resultFieldName!! to KotlinType(resultTypeString ?: DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(resultType!!))
    }

    return makeOtherScripts(ktScript).onSuccess { otherScripts ->
        KJvmCompiledScript(
            script.locationId,
            getScriptConfiguration(ktScript.containingKtFile),
            ktScript.fqName.asString(),
            resultField,
            otherScripts,
            module
        ).asSuccess()
    }
}

