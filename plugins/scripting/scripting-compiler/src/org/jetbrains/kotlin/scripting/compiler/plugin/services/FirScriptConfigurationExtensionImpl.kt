/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirScriptBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.definitions.annotationsForSamWithReceivers
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.StringScriptSource


class FirScriptConfiguratorExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove suppression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : FirScriptConfiguratorExtension(session) {

    override fun FirScriptBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
    }

    // TODO: find out some way to differentiate detection form REPL snippets, to allow reporting conflicts on FIR building
    override fun accepts(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean =
        sourceFile != null && // this implementation requires a file to find definition (this could be relaxed eventually)
                (scriptSource is KtPsiSourceElement && scriptSource.psi is KtScript) // workd only with PSI so far

    @OptIn(SymbolInternals::class)
    override fun FirScriptBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>) {
        val configuration = getOrLoadConfiguration(session, sourceFile!!) ?: run {
            log.warn("Configuration for ${sourceFile.asString()} wasn't found. FirScriptBuilder wasn't configured.")
            return
        }

        configuration.getNoDefault(ScriptCompilationConfiguration.baseClass)?.let { baseClass ->
            val baseClassTypeRef =
                tryResolveOrBuildParameterTypeRefFromKotlinType(baseClass, source?.fakeElement(KtFakeSourceElementKind.ScriptBaseClass))

            receivers.add(
                buildScriptReceiverParameter {
                    typeRef = baseClassTypeRef
                    isBaseClassReceiver = true
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
                    containingDeclarationSymbol = this@configure.symbol
                }
            )

            if (baseClassTypeRef is FirResolvedTypeRef) {
                baseClassTypeRef.toRegularClassSymbol(session)?.fir?.primaryConstructorIfAny(session)?.fir?.valueParameters?.forEach { baseCtorParameter ->
                    parameters.add(
                        buildProperty {
                            moduleData = session.moduleData
                            source = this@configure.source?.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                            origin = FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
                            // TODO: copy type parameters?
                            returnTypeRef = baseCtorParameter.returnTypeRef
                            name = baseCtorParameter.name
                            symbol = FirPropertySymbol(name)
                            status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                            isLocal = true
                            isVar = false
                        }
                    )
                }
            }
        }

        configuration[ScriptCompilationConfiguration.implicitReceivers]?.forEach { implicitReceiver ->
            receivers.add(
                buildScriptReceiverParameter {
                    typeRef = this@configure.tryResolveOrBuildParameterTypeRefFromKotlinType(implicitReceiver)
                    isBaseClassReceiver = false
                    symbol = FirReceiverParameterSymbol()
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    containingDeclarationSymbol = this@configure.symbol
                }
            )
        }

        configuration[ScriptCompilationConfiguration.providedProperties]?.forEach { (propertyName, propertyType) ->
            parameters.add(
                buildProperty {
                    moduleData = session.moduleData
                    source = this@configure.source?.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    returnTypeRef = this@configure.tryResolveOrBuildParameterTypeRefFromKotlinType(propertyType)
                    name = Name.identifier(propertyName)
                    symbol = FirPropertySymbol(name)
                    status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    isLocal = true
                    isVar = false
                }
            )
        }

        configuration[ScriptCompilationConfiguration.annotationsForSamWithReceivers]?.forEach {
            _knownAnnotationsForSamWithReceiver.add(it.typeName)
        }

        configuration[ScriptCompilationConfiguration.resultField]?.takeIf { it.isNotBlank() }?.let { resultFieldName ->
            val lastScriptBlock = declarations.lastOrNull() as? FirAnonymousInitializer
            val lastExpression =
                when (val lastScriptBlockBody = lastScriptBlock?.body) {
                    is FirLazyBlock -> null
                    is FirSingleExpressionBlock -> lastScriptBlockBody.statement as? FirExpression
                    else -> lastScriptBlockBody?.statements?.single()?.takeIf { it is FirExpression } as? FirExpression
                }?.takeUnless { it is FirErrorExpression }

            if (lastExpression != null) {
                declarations.removeLast()
                @OptIn(UnresolvedExpressionTypeAccess::class)
                val lastExpressionTypeRef =
                    lastExpression.takeUnless { it is FirLazyExpression }?.coneTypeOrNull?.toFirResolvedTypeRef()
                        ?: FirImplicitTypeRefImplWithoutSource
                declarations.add(
                    buildProperty {
                        this.name = Name.identifier(resultFieldName)
                        this.symbol = FirPropertySymbol(CallableId(context.packageFqName, this.name))
                        source = lastScriptBlock?.source
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.ScriptCustomization.ResultProperty
                        initializer = lastExpression
                        returnTypeRef = lastExpressionTypeRef
                        getter = FirDefaultPropertyGetter(
                            lastScriptBlock?.source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                            session.moduleData,
                            FirDeclarationOrigin.ScriptCustomization.ResultProperty,
                            lastExpressionTypeRef,
                            Visibilities.Public,
                            this.symbol,
                        )
                        status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                        isLocal = false
                        isVar = false
                    }.also {
                        resultPropertyName = it.name
                    }
                )
            }
        }
    }

    private fun KtSourceFile.asString() = path ?: name

    private fun FirScriptBuilder.tryResolveOrBuildParameterTypeRefFromKotlinType(
        kotlinType: KotlinType,
        sourceElement: KtSourceElement? = source?.fakeElement(KtFakeSourceElementKind.ScriptParameter),
    ): FirTypeRef {
        // TODO: check/support generics and other cases (KT-72638)
        // such a conversion by simple splitting by a '.', is overly simple and does not support all cases, e.g. generics or backticks
        // but to support it properly, one may need to reimplement or reuse types paring code
        // but since such cases a considered exotic, it is not implemented yet.
        val fqName = FqName.fromSegments(kotlinType.typeName.split("."))
        val classId = ClassId(fqName.parent(), fqName.shortName())
        val classFromDeps = session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)
        return if (classFromDeps != null) {
            buildResolvedTypeRef {
                source = sourceElement
                coneType = classFromDeps.constructType(isMarkedNullable = kotlinType.isNullable)
            }
        } else {
            buildUserTypeRef {
                source = sourceElement
                isMarkedNullable = kotlinType.isNullable
                qualifier.addAll(
                    fqName.pathSegments().map {
                        FirQualifierPartImpl(null, it, FirTypeArgumentListImpl(null))
                    }
                )
            }
        }
    }

    private val _knownAnnotationsForSamWithReceiver = hashSetOf<String>()

    internal val knownAnnotationsForSamWithReceiver: Set<String>
        get() = _knownAnnotationsForSamWithReceiver

    companion object {
        private val log: Logger get() = Logger.getInstance(FirScriptConfiguratorExtensionImpl::class.java)

        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirScriptConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

private fun SourceCode.originalKtFile(): KtFile =
    (this as? KtFileScriptSource)?.ktFile?.originalFile as? KtFile
        ?: error("only PSI scripts are supported at the moment")

private fun FirScriptDefinitionProviderService.configurationFor(file: KtFile): ScriptCompilationConfiguration? =
    configurationProvider?.getScriptConfigurationResult(file)?.valueOrNull()?.configuration

private fun FirScriptDefinitionProviderService.configurationFor(sourceCode: SourceCode): ScriptCompilationConfiguration? =
    definitionProvider?.findDefinition(sourceCode)?.compilationConfiguration

private fun FirScriptDefinitionProviderService.defaultConfiguration(): ScriptCompilationConfiguration? =
    definitionProvider?.getDefaultDefinition()?.compilationConfiguration

fun KtSourceFile.toSourceCode(): SourceCode? = when (this) {
    is KtPsiSourceFile -> (psiFile as? KtFile)?.let(::KtFileScriptSource) ?: VirtualFileScriptSource(psiFile.virtualFile)
    is KtVirtualFileSourceFile -> VirtualFileScriptSource(virtualFile)
    is KtIoFileSourceFile -> FileScriptSource(file)
    is KtInMemoryTextSourceFile -> StringScriptSource(text.toString(), name)
    else -> null
}

internal fun getOrLoadConfiguration(session: FirSession, file: KtSourceFile): ScriptCompilationConfiguration? {
    val service = checkNotNull(session.scriptDefinitionProviderService)
    val sourceCode = file.toSourceCode()
    val ktFile = sourceCode?.originalKtFile()
    val configuration = with(service) {
        ktFile?.let { asKtFile -> configurationFor(asKtFile) }
            ?: sourceCode?.let { asSourceCode -> configurationFor(asSourceCode) }
            ?: defaultConfiguration()
    }
    return configuration
}

