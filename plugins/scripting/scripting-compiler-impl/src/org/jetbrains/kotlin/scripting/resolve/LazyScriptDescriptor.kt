/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.MISSING_IMPORTED_SCRIPT_FILE
import org.jetbrains.kotlin.diagnostics.Errors.MISSING_IMPORTED_SCRIPT_PSI
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.resolve.AllUnderImportScope
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.utils.addImportingScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities
import org.jetbrains.kotlin.scripting.definitions.findScriptCompilationConfiguration
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.GetScriptingClass
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.toValidJvmIdentifier


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

    private val _resultValue: () -> ReplResultPropertyDescriptor? = resolveSession.storageManager.createNullableLazyValue {
        val expression = scriptInfo.script
            .getChildOfType<KtBlockExpression>()
            ?.getChildrenOfType<KtScriptInitializer>()?.lastOrNull()
            ?.getChildOfType<KtExpression>()

        val type = expression?.let {
            resolveSession.trace.bindingContext.getType(it)
        }

        if (type != null && !type.isUnit() && !type.isNothing()) {
            resultFieldName()?.let {
                ReplResultPropertyDescriptor(
                    it,
                    type,
                    this.thisAsReceiverParameter,
                    this,
                    expression.toSourceElement()
                )
            }
        } else null
    }

    override fun getResultValue(): ReplResultPropertyDescriptor? = _resultValue()

    fun resultFieldName(): Name? {
        // TODO: implement robust REPL/script selection
        val replSnippetId =
            scriptInfo.script.getUserData(ScriptPriorities.PRIORITY_KEY)?.toString()
        val identifier = if (replSnippetId != null) {
            // assuming repl
            scriptCompilationConfiguration()[ScriptCompilationConfiguration.repl.resultFieldPrefix]?.takeIf { it.isNotBlank() }?.let {
                "$it$replSnippetId"
            }
        } else {
            scriptCompilationConfiguration()[ScriptCompilationConfiguration.resultField]?.takeIf { it.isNotBlank() }
        }
        return identifier?.let { Name.identifier(it) }
    }

    private val sourceElement = scriptInfo.script.toSourceElement()

    override fun getSource() = sourceElement

    private val priority: Int = ScriptPriorities.getScriptPriority(scriptInfo.script)

    override fun getPriority() = priority

    val scriptCompilationConfiguration: () -> ScriptCompilationConfiguration = resolveSession.storageManager.createLazyValue {
        scriptInfo.script.containingKtFile.findScriptCompilationConfiguration()
            ?: throw IllegalArgumentException("Unable to find script compilation configuration for the script ${scriptInfo.script.containingFile}")
    }

    private val scriptingHostConfiguration: () -> ScriptingHostConfiguration = resolveSession.storageManager.createLazyValue {
        // TODO: use platform-specific configuration by default instead
        scriptCompilationConfiguration()[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
    }

    private val scriptingClassGetter: () -> GetScriptingClass = resolveSession.storageManager.createLazyValue {
        scriptingHostConfiguration()[ScriptingHostConfiguration.getScriptingClass]
            ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting host configuration for the script ${scriptInfo.script.containingFile}")
    }

    fun getScriptingClass(type: KotlinType): KClass<*> =
        scriptingClassGetter()(
            type,
            ScriptDefinition::class, // Assuming that the ScriptDefinition class is loaded in the proper classloader, TODO: consider more reliable way to load or cache classes
            scriptingHostConfiguration()
        )

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
        visitor.visitScriptDescriptor(this, data)

    override fun createScopesHolderForClass(
        c: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider
    ): ScopesHolderForClass<LazyClassMemberScope> =
        ScopesHolderForClass.create(this, c.storageManager, c.kotlinTypeCheckerOfOwnerModule.kotlinTypeRefiner) {
            LazyScriptClassMemberScope(
                // Must be a ResolveSession for scripts
                c as ResolveSession,
                declarationProvider,
                this,
                c.trace
            )
        }

    override fun getUnsubstitutedPrimaryConstructor() = super.getUnsubstitutedPrimaryConstructor()!!

    internal val baseClassDescriptor: () -> ClassDescriptor? = resolveSession.storageManager.createNullableLazyValue {
        val scriptBaseType = scriptCompilationConfiguration()[ScriptCompilationConfiguration.baseClass]
            ?: error("Base class is not configured for the script ${scriptInfo.script.containingFile}")
        val typeName = scriptBaseType.run { fromClass?.toString()?.replace("class ", "") ?: typeName }
        val fqnName = FqName(typeName)
        val classId = ClassId.topLevel(fqnName)

        findTypeDescriptor(
            classId,
            typeName,
            if (fqnName.parent().asString().startsWith("kotlin.script.templates.standard")) Errors.MISSING_SCRIPT_STANDARD_TEMPLATE
            else Errors.MISSING_SCRIPT_BASE_CLASS
        )
    }

    override fun computeSupertypes() = listOf(baseClassDescriptor()?.defaultType ?: builtIns.anyType)

    private inner class ImportedScriptDescriptorsFinder {

        val localFS: VirtualFileSystem by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val fileManager = VirtualFileManager.getInstance()
            fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        }
        val psiManager by lazy(LazyThreadSafetyMode.PUBLICATION) { PsiManager.getInstance(scriptInfo.script.project) }

        operator fun invoke(importedScript: SourceCode): ScriptDescriptor? {
            // Note: is not an error now - if import references other valid source file, it is simply compiled along with script
            // TODO: check if this is the behavior we want to have - see #KT-28916
            val ktScript = getKtFile(importedScript)?.declarations?.firstIsInstanceOrNull<KtScript>()
                ?: return null
            return resolveSession.getScriptDescriptor(ktScript) as ScriptDescriptor
        }

        private fun getKtFile(script: SourceCode): KtFile? {
            if (script is KtFileScriptSource) return script.ktFile

            // TODO: support any kind of ScriptSource.
            if (script !is FileBasedScriptSource) return null

            fun errorKtFile(errorDiagnostic: DiagnosticFactory1<PsiElement, String>?): KtFile? {
                reportErrorString1(errorDiagnostic, script.file.path)
                return null
            }

            val virtualFile = when (script) {
                is VirtualFileScriptSource -> script.virtualFile
                else -> localFS.findFileByPath(script.file.absolutePath) ?: return errorKtFile(MISSING_IMPORTED_SCRIPT_FILE)
            }

            val psiFile = psiManager.findFile(virtualFile) ?: return errorKtFile(MISSING_IMPORTED_SCRIPT_PSI)
            return psiFile as? KtFile
        }
    }

    private val scriptImplicitReceivers: () -> List<ClassDescriptor> = resolveSession.storageManager.createLazyValue {
        val res = ArrayList<ClassDescriptor>()

        val importedScriptsFiles = ScriptDependenciesProvider.getInstance(scriptInfo.script.project)
            ?.getScriptConfiguration(scriptInfo.script.containingKtFile)?.importedScripts
        if (importedScriptsFiles != null) {
            val findImportedScriptDescriptor = ImportedScriptDescriptorsFinder()
            importedScriptsFiles.mapNotNullTo(res) {
                findImportedScriptDescriptor(it)
            }
        }

        scriptCompilationConfiguration()[ScriptCompilationConfiguration.implicitReceivers]?.mapNotNullTo(res) { receiver ->
            findTypeDescriptor(getScriptingClass(receiver), Errors.MISSING_SCRIPT_RECEIVER_CLASS)
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
                    scriptInfo.script.containingFile,
                    arg
                )
            )
        }
    }

    override fun getImplicitReceivers(): List<ClassDescriptor> = scriptImplicitReceivers()

    private val scriptProvidedProperties: () -> List<ScriptProvidedPropertyDescriptor> = resolveSession.storageManager.createLazyValue {
        scriptCompilationConfiguration()[ScriptCompilationConfiguration.providedProperties].orEmpty()
            .mapNotNull { (name, type) ->
                findTypeDescriptor(getScriptingClass(type), Errors.MISSING_SCRIPT_PROVIDED_PROPERTY_CLASS)
                    ?.let { name.toValidJvmIdentifier() to it }
            }.map { (name, classDescriptor) ->
                ScriptProvidedPropertyDescriptor(
                    Name.identifier(name),
                    classDescriptor,
                    thisAsReceiverParameter,
                    true,
                    this
                )
            }
    }

    override fun getScriptProvidedProperties(): List<PropertyDescriptor> = scriptProvidedProperties()

    internal class ConstructorWithParams(
        val constructor: ClassConstructorDescriptorImpl,
        val baseClassConstructorParameters: List<ValueParameterDescriptor>,
        val implicitReceiversParameters: List<ValueParameterDescriptor>,
        val scriptProvidedPropertiesParameters: List<ValueParameterDescriptor>
    )

    internal val scriptPrimaryConstructorWithParams: () -> ConstructorWithParams = resolveSession.storageManager.createLazyValue {
        val baseConstructorDescriptor = baseClassDescriptor()?.unsubstitutedPrimaryConstructor
        val inheritedAnnotations = baseConstructorDescriptor?.annotations ?: Annotations.EMPTY
        val baseExplicitParameters = baseConstructorDescriptor?.valueParameters ?: emptyList()

        val implicitReceiversParamTypes =
            implicitReceivers.mapIndexed { idx, receiver ->
                val receiverName =
                    if (receiver is ScriptDescriptor) "${LazyScriptClassMemberScope.IMPORTED_SCRIPT_PARAM_NAME_PREFIX}${receiver.name}"
                    else "${LazyScriptClassMemberScope.IMPLICIT_RECEIVER_PARAM_NAME_PREFIX}$idx"
                receiverName to receiver.defaultType
            }

        val providedPropertiesParamTypes =
            scriptProvidedProperties().map {
                it.name.identifier to it.type
            }
        val constructorDescriptor = ClassConstructorDescriptorImpl.create(this, inheritedAnnotations, true, source)

        var paramsIndexBase = baseExplicitParameters.lastIndex + 1

        fun createValueParameter(param: Pair<String, org.jetbrains.kotlin.types.KotlinType>) =
            ValueParameterDescriptorImpl(
                constructorDescriptor,
                null,
                paramsIndexBase++,
                Annotations.EMPTY,
                Name.identifier(param.first),
                param.second,
                declaresDefaultValue = false, isCrossinline = false, isNoinline = false, varargElementType = null,
                source = SourceElement.NO_SOURCE
            )

        val explicitParameters = baseExplicitParameters.map { it.copy(constructorDescriptor, it.name, it.index) }
        val implicitReceiversParameters = implicitReceiversParamTypes.map(::createValueParameter)
        val providedPropertiesParameters = providedPropertiesParamTypes.map(::createValueParameter)

        constructorDescriptor.initialize(
            explicitParameters + implicitReceiversParameters + providedPropertiesParameters, DescriptorVisibilities.PUBLIC
        )
        constructorDescriptor.returnType = defaultType()

        ConstructorWithParams(
            constructorDescriptor,
            baseClassConstructorParameters = explicitParameters,
            implicitReceiversParameters = implicitReceiversParameters,
            scriptProvidedPropertiesParameters = providedPropertiesParameters
        )
    }

    override fun getExplicitConstructorParameters(): List<ValueParameterDescriptor> =
        scriptPrimaryConstructorWithParams().baseClassConstructorParameters

    override fun getImplicitReceiversParameters(): List<ValueParameterDescriptor> =
        scriptPrimaryConstructorWithParams().implicitReceiversParameters

    override fun getScriptProvidedPropertiesParameters(): List<ValueParameterDescriptor> =
        scriptPrimaryConstructorWithParams().scriptProvidedPropertiesParameters

    private val scriptOuterScope: () -> LexicalScope = resolveSession.storageManager.createLazyValue {
        var outerScope = super.getOuterScope()
        for (receiverClassDescriptor in implicitReceivers.asReversed()) {
            outerScope = LexicalScopeImpl(
                outerScope,
                receiverClassDescriptor,
                true,
                listOf(receiverClassDescriptor.thisAsReceiverParameter),
                LexicalScopeKind.CLASS_MEMBER_SCOPE
            ).addImportingScope(
                AllUnderImportScope.create(receiverClassDescriptor, emptyList())
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
