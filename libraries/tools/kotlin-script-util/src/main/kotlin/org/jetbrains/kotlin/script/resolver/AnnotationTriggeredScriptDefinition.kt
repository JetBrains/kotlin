/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script.resolver


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

open class AnnotationTriggeredScriptDefinition(definitionName: String,
                                               template: KClass<out Any>,
                                               defaultEmptyArgs: ScriptArgsWithTypes? = null,
                                               val resolvers: List<AnnotationTriggeredScriptResolver> = emptyList(),
                                               defaultImports: List<String> = emptyList(),
                                               val scriptFilePattern: Regex = DEFAULT_SCRIPT_FILE_PATTERN.toRegex(),
                                               val environment: Map<String, Any?>? = null) :
        KotlinScriptDefinitionEx(template, defaultEmptyArgs, defaultImports) {
    override val name = definitionName
    protected val acceptedAnnotations = resolvers.map { it.acceptedAnnotations }.flatten()

    override fun <TF : Any> isScript(file: TF): Boolean = getFileName(file).endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    protected val resolutionManager: AnnotationTriggeredResolutionManager by lazy {
        AnnotationTriggeredResolutionManager(resolvers)
    }

    override fun getScriptName(script: KtScript): Name = NameUtils.getScriptNameForFile(script.containingKtFile.name)

    class MergeDependencies(val current: KotlinScriptExternalDependencies, val backup: KotlinScriptExternalDependencies) : KotlinScriptExternalDependencies {
        override val imports: List<String> get() = (current.imports + backup.imports).distinct()
        override val javaHome: String? get() = current.javaHome ?: backup.javaHome
        override val classpath: Iterable<File> get() = (current.classpath + backup.classpath).distinct()
        override val sources: Iterable<File> get() = (current.sources + backup.sources).distinct()
        override val scripts: Iterable<File> get() = (current.scripts + backup.scripts).distinct()
    }

    override fun <TF : Any> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
        fun logClassloadingError(ex: Throwable) {
            logScriptDefMessage(ScriptDependenciesResolver.ReportSeverity.WARNING, ex.message ?: "Invalid script template: ${template.qualifiedName}", null)
        }

        @Suppress("UNCHECKED_CAST")
        fun makeScriptContents() = BasicScriptContents(file, getAnnotations = {
            val classLoader = (template as Any).javaClass.classLoader
            try {
                getAnnotationEntries(file, project)
                        .mapNotNull { psiAnn ->
                            // TODO: consider advanced matching using semantic similar to actual resolving
                            acceptedAnnotations.find { ann ->
                                psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
                            }?.let { constructAnnotation(psiAnn, classLoader.loadClass(it.qualifiedName).kotlin as KClass<out Annotation>) }
                        }
            }
            catch (ex: Throwable) {
                logClassloadingError(ex)
                emptyList()
            }
        })

        try {
            val fileDeps = resolutionManager.resolve(makeScriptContents(), environment, Companion::logScriptDefMessage, previousDependencies)
            // TODO: use it as a Future
            val updatedDependencies = fileDeps.get()
            val backupDependencies = super.getDependenciesFor(file, project, previousDependencies)
            return if (updatedDependencies == null || backupDependencies == null) updatedDependencies ?: backupDependencies
            else MergeDependencies(updatedDependencies, backupDependencies)
        }
        catch (ex: Throwable) {
            logClassloadingError(ex)
        }
        return null
    }

    private val KtAnnotationEntry.typeName: String get() = (typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

    internal fun String?.orAnonymous(kind: String = ""): String =
            this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"

    internal fun constructAnnotation(psi: KtAnnotationEntry, targetClass: KClass<out Annotation>): Annotation {

        val valueArguments = psi.valueArguments.map { arg ->
            val evaluator = ConstantExpressionEvaluator(DefaultBuiltIns.Instance, LanguageVersionSettingsImpl.DEFAULT)
            val trace = BindingTraceContext()
            val result = evaluator.evaluateToConstantValue(arg.getArgumentExpression()!!, trace, TypeUtils.NO_EXPECTED_TYPE)
            // TODO: consider inspecting `trace` to find diagnostics reported during the computation (such as division by zero, integer overflow, invalid annotation parameters etc.)
            val argName = arg.getArgumentName()?.asName?.toString()
            argName to result?.value
        }
        val mappedArguments: Map<KParameter, Any?> =
                tryCreateCallableMappingFromNamedArgs(targetClass.constructors.first(), valueArguments)
                ?: return InvalidScriptResolverAnnotation(psi.typeName, valueArguments)

        try {
            return targetClass.primaryConstructor!!.callBy(mappedArguments)
        }
        catch (ex: Exception) {
            return InvalidScriptResolverAnnotation(psi.typeName, valueArguments, ex)
        }
    }

    class InvalidScriptResolverAnnotation(val name: String, val annParams: List<Pair<String?, Any?>>?, val error: Exception? = null) : Annotation


    private fun <TF : Any> getAnnotationEntries(file: TF, project: Project): Iterable<KtAnnotationEntry> = when (file) {
        is PsiFile -> getAnnotationEntriesFromPsiFile(file)
        is VirtualFile -> getAnnotationEntriesFromVirtualFile(file, project)
        is File -> {
            val virtualFile = (StandardFileSystems.local().findFileByPath(file.canonicalPath)
                               ?: throw IllegalArgumentException("Unable to find file ${file.canonicalPath}"))
            getAnnotationEntriesFromVirtualFile(virtualFile, project)
        }
        else -> throw IllegalArgumentException("Unsupported file type $file")
    }

    private fun getAnnotationEntriesFromPsiFile(file: PsiFile) =
            (file as? KtFile)?.annotationEntries
            ?: throw IllegalArgumentException("Unable to extract kotlin annotations from ${file.name} (${file.fileType})")

    private fun getAnnotationEntriesFromVirtualFile(file: VirtualFile, project: Project): Iterable<KtAnnotationEntry> {
        val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                               ?: throw IllegalArgumentException("Unable to load PSI from ${file.canonicalPath}")
        return getAnnotationEntriesFromPsiFile(psiFile)
    }

    class BasicScriptContents<out TF : Any>(myFile: TF, getAnnotations: () -> Iterable<Annotation>) : ScriptContents {
        override val file: File? by lazy { getFile(myFile) }
        override val annotations: Iterable<Annotation> by lazy { getAnnotations() }
        override val text: CharSequence? by lazy { getFileContents(myFile) }
    }

    companion object {
        internal val log = Logger.getInstance(KotlinScriptDefinitionFromAnnotatedTemplate::class.java)

        fun logScriptDefMessage(reportSeverity: ScriptDependenciesResolver.ReportSeverity, s: String, position: ScriptContents.Position?): Unit {
            val msg = (position?.run { "[at $line:$col]" } ?: "") + s
            when (reportSeverity) {
                ScriptDependenciesResolver.ReportSeverity.ERROR -> log.error(msg)
                ScriptDependenciesResolver.ReportSeverity.WARNING -> log.warn(msg)
                ScriptDependenciesResolver.ReportSeverity.INFO -> log.info(msg)
                ScriptDependenciesResolver.ReportSeverity.DEBUG -> log.debug(msg)
            }
        }
    }
}
