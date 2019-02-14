/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.resolve

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.MISSING_IMPORTED_SCRIPT_FILE
import org.jetbrains.kotlin.diagnostics.Errors.MISSING_IMPORTED_SCRIPT_PSI
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.script.ScriptPriorities
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptDefinitionByFileName
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType


class LazyScriptDescriptor(
    val resolveSession: ResolveSession,
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    internal val scriptInfo: KtScriptInfo
) : ScriptDescriptor, LazyClassDescriptor(
    resolveSession,
    containingDeclaration,
    name,
    scriptInfo,
    /* isExternal = */ false
) {
    init {
        resolveSession.trace.record(BindingContext.SCRIPT, scriptInfo.script, this)
    }

    override fun getResultValue(): ReplResultPropertyDescriptor? {
        val expression = scriptInfo.script
            .getChildOfType<KtBlockExpression>()
            ?.getChildrenOfType<KtScriptInitializer>()?.lastOrNull()
            ?.getChildOfType<KtExpression>()

        val type = expression?.let {
            resolveSession.trace.bindingContext.getType(it)
        }

        return if (type != null && !type.isUnit() && !type.isNothing()) {
            resultFieldName()?.let {
                ReplResultPropertyDescriptor(
                    Name.identifier(it),
                    type,
                    this.thisAsReceiverParameter,
                    this,
                    expression.toSourceElement()
                )
            }
        } else null
    }

    fun resultFieldName(): String? {
        val scriptPriority = scriptInfo.script.getUserData(ScriptPriorities.PRIORITY_KEY)
        if (scriptPriority != null) {
            return "res$scriptPriority"
        }
        val scriptName = name.asString()
        return if (scriptName.startsWith("Line_")) {
            "res${scriptName.split("_")[1]}"
        } else "\$\$result"
    }

    private val sourceElement = scriptInfo.script.toSourceElement()

    override fun getSource() = sourceElement

    private val priority: Int = ScriptPriorities.getScriptPriority(scriptInfo.script)

    override fun getPriority() = priority

    val scriptDefinition: () -> KotlinScriptDefinition = resolveSession.storageManager.createLazyValue {
        scriptDefinitionByFileName(resolveSession.project, scriptInfo.script.containingKtFile.name)
    }

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitScriptDescriptor(this, data)

    override fun createMemberScope(c: LazyClassContext, declarationProvider: ClassMemberDeclarationProvider): LazyScriptClassMemberScope =
        LazyScriptClassMemberScope(
            // Must be a ResolveSession for scripts
            c as ResolveSession,
            declarationProvider,
            this,
            c.trace
        )

    override fun getUnsubstitutedPrimaryConstructor() = super.getUnsubstitutedPrimaryConstructor()!!

    internal val baseClassDescriptor: () -> ClassDescriptor? = resolveSession.storageManager.createNullableLazyValue {
        val template = scriptDefinition().template
        findTypeDescriptor(
            template,
            if (template.qualifiedName?.startsWith("kotlin.script.templates.standard") == true) Errors.MISSING_SCRIPT_STANDARD_TEMPLATE
            else Errors.MISSING_SCRIPT_BASE_CLASS
        )
    }

    override fun computeSupertypes() = listOf(baseClassDescriptor()?.defaultType ?: builtIns.anyType)

    private inner class ImportedScriptDescriptorsFinder {

        val fileManager = VirtualFileManager.getInstance()
        val localFS = fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val psiManager = PsiManager.getInstance(scriptInfo.script.project)

        operator fun invoke(importedScriptFile: File): ScriptDescriptor? {

            fun errorDescriptor(errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): ScriptDescriptor? {
                reportErrorString1(errorDiagnostic, importedScriptFile.path)
                return null
            }

            val vfile = localFS.findFileByPath(importedScriptFile.path)
                ?: return errorDescriptor(MISSING_IMPORTED_SCRIPT_FILE)
            val psiFile = psiManager.findFile(vfile)
                ?: return errorDescriptor(MISSING_IMPORTED_SCRIPT_PSI)
            // Note: is not an error now - if import references other valid source file, it is simply compiled along with script
            // TODO: check if this is the behavior we want to have - see #KT-28916
            val ktScript = (psiFile as? KtFile)?.declarations?.firstIsInstanceOrNull<KtScript>()
                ?: return null
            return resolveSession.getScriptDescriptor(ktScript) as ScriptDescriptor
        }
    }

    private val scriptImplicitReceivers: () -> List<ClassDescriptor> = resolveSession.storageManager.createLazyValue {
        val res = ArrayList<ClassDescriptor>()

        val importedScriptsFiles = ScriptDependenciesProvider.getInstance(scriptInfo.script.project)
            ?.getScriptDependencies(scriptInfo.script.containingKtFile)?.scripts
        if (importedScriptsFiles != null) {
            val findImportedScriptDescriptor = ImportedScriptDescriptorsFinder()
            importedScriptsFiles.mapNotNullTo(res) {
                findImportedScriptDescriptor(it)
            }
        }

        scriptDefinition().implicitReceivers.mapNotNullTo(res) { receiver ->
            findTypeDescriptor(receiver, Errors.MISSING_SCRIPT_RECEIVER_CLASS)
        }

        res
    }

    internal fun findTypeDescriptor(kClass: KClass<*>, errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): ClassDescriptor? =
        findTypeDescriptor(kClass.classId, kClass.toString(), errorDiagnostic)

    internal fun findTypeDescriptor(type: KType, errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): ClassDescriptor? =
        findTypeDescriptor(type.classId, type.toString(), errorDiagnostic)

    internal fun findTypeDescriptor(
        classId: ClassId?, typeName: String,
        errorDiagnostic: DiagnosticFactory1<PsiElement, String>?
    ): ClassDescriptor? {
        val typeDescriptor = classId?.let { module.findClassAcrossModuleDependencies(it) }
        if (typeDescriptor == null) {
            reportErrorString1(errorDiagnostic, classId?.asSingleFqName()?.toString() ?: typeName)
        }
        return typeDescriptor
    }

    private fun reportErrorString1(errorDiagnostic: DiagnosticFactory1<PsiElement, String>?, arg: String) {
        if (errorDiagnostic != null) {
            // TODO: use PositioningStrategies to highlight some specific place in case of error, instead of treating the whole file as invalid
            resolveSession.trace.report(
                errorDiagnostic.on(
                    scriptInfo.script,
                    arg
                )
            )
        }
    }

    override fun getImplicitReceivers(): List<ClassDescriptor> = scriptImplicitReceivers()

    private val scriptProvidedProperties: () -> ScriptProvidedPropertiesDescriptor = resolveSession.storageManager.createLazyValue {
        ScriptProvidedPropertiesDescriptor(this)
    }

    override fun getScriptProvidedProperties(): List<PropertyDescriptor> = scriptProvidedProperties().properties()

    private val scriptOuterScope: () -> LexicalScope = resolveSession.storageManager.createLazyValue {
        var outerScope = super.getOuterScope()
        val outerScopeReceivers = implicitReceivers.let {
            if (scriptDefinition().providedProperties.isEmpty()) {
                it
            } else {
                it + ScriptProvidedPropertiesDescriptor(this)
            }
        }
        for (receiverClassDescriptor in outerScopeReceivers.asReversed()) {
            outerScope = LexicalScopeImpl(
                outerScope,
                receiverClassDescriptor,
                true,
                receiverClassDescriptor.thisAsReceiverParameter,
                LexicalScopeKind.CLASS_MEMBER_SCOPE
            )
        }
        outerScope
    }

    override fun getOuterScope(): LexicalScope = scriptOuterScope()

    private val scriptClassAnnotations: () -> Annotations = resolveSession.storageManager.createLazyValue {
        baseClassDescriptor()?.annotations?.let { ann ->
            FilteredAnnotations(ann) { fqname ->
                val shortName = fqname.shortName().identifier
                // TODO: consider more precise annotation filtering
                !shortName.startsWith("KotlinScript") && !shortName.startsWith("ScriptTemplate")
            }
        } ?: super.annotations
    }

    override val annotations: Annotations
        get() = scriptClassAnnotations()
}
