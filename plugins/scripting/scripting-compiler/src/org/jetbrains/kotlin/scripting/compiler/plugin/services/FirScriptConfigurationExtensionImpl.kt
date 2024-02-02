/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension.Factory
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.SCRIPT_SPECIAL_NAME_STRING
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.annotationsForSamWithReceivers
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.StringScriptSource


class FirScriptConfiguratorExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove supression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER") hostConfiguration: ScriptingHostConfiguration,
) : FirScriptConfiguratorExtension(session) {

    @OptIn(SymbolInternals::class)
    override fun FirScriptBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
        val sourceFile = fileBuilder.sourceFile ?: return
        val configuration = getOrLoadConfiguration(sourceFile) ?: run {
            log.warn("Configuration for ${sourceFile.asString()} wasn't found. FirScriptBuilder wasn't configured.")
            return
        }

        configuration[ScriptCompilationConfiguration.defaultImports]?.forEach { defaultImport ->
            val trimmed = defaultImport.trim()
            val endsWithStar = trimmed.endsWith("*")
            val stripped = if (endsWithStar) trimmed.substring(0, trimmed.length - 2) else trimmed
            val fqName = FqName.fromSegments(stripped.split("."))
            fileBuilder.imports += buildImport {
                source = fileBuilder.source?.fakeElement(KtFakeSourceElementKind.ImplicitImport)
                importedFqName = fqName
                isAllUnder = endsWithStar
            }
        }
    }

    @OptIn(SymbolInternals::class)
    override fun FirScriptBuilder.configure(sourceFile: KtSourceFile) {
        val configuration = getOrLoadConfiguration(sourceFile) ?: run {
            log.warn("Configuration for ${sourceFile.asString()} wasn't found. FirScriptBuilder wasn't configured.")
            return
        }

        // TODO: rewrite/extract decision logic for clarity
        configuration.getNoDefault(ScriptCompilationConfiguration.baseClass)?.let { baseClass ->
            val baseClassFqn = FqName.fromSegments(baseClass.typeName.split("."))
            contextReceivers.add(buildContextReceiverWithFqName(baseClassFqn, Name.special(SCRIPT_SPECIAL_NAME_STRING)))

            val baseClassSymbol =
                session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(ClassId(baseClassFqn.parent(), baseClassFqn.shortName()))
                        as? FirRegularClassSymbol
            if (baseClassSymbol != null) {
                // assuming that if base class will be unresolved, the error will be reported on the contextReceiver
                baseClassSymbol.fir.primaryConstructorIfAny(session)?.fir?.valueParameters?.forEach { baseCtorParameter ->
                    parameters.add(
                        buildProperty {
                            moduleData = session.moduleData
                            source = this@configure.source?.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                            origin = FirDeclarationOrigin.ScriptCustomization.Parameter
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
            contextReceivers.add(buildContextReceiverWithFqName(FqName.fromSegments(implicitReceiver.typeName.split("."))))
        }

        configuration[ScriptCompilationConfiguration.providedProperties]?.forEach { (propertyName, propertyType) ->
            val typeRef = buildUserTypeRef {
                isMarkedNullable = propertyType.isNullable
                propertyType.typeName.split(".").forEach {
                    qualifier.add(FirQualifierPartImpl(null, Name.identifier(it), FirTypeArgumentListImpl(null)))
                }
            }
            parameters.add(
                buildProperty {
                    moduleData = session.moduleData
                    source = this@configure.source?.fakeElement(KtFakeSourceElementKind.ScriptParameter)
                    origin = FirDeclarationOrigin.ScriptCustomization.Parameter
                    returnTypeRef = typeRef
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
                        this.symbol = FirPropertySymbol(this.name)
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

    private fun getOrLoadConfiguration(file: KtSourceFile): ScriptCompilationConfiguration? {
        val service = checkNotNull(session.scriptDefinitionProviderService)
        val sourceCode = file.toSourceCode()
        val ktFile = sourceCode?.originalKtFile()
        val configuration = with(service) {
            ktFile?.let { asKtFile -> configurationFor(asKtFile) }
                ?: sourceCode?.let { asSourceCode -> configurationFor(asSourceCode) }
                ?: defaultConfiguration()?.also { log.debug("Default configuration loaded for ${file.asString()}") }
        }
        return configuration
    }

    private fun buildContextReceiverWithFqName(classFqn: FqName, customName: Name? = null) =
        buildContextReceiver {
            typeRef = buildUserTypeRef {
                isMarkedNullable = false
                qualifier.addAll(
                    classFqn.pathSegments().map {
                        FirQualifierPartImpl(null, it, FirTypeArgumentListImpl(null))
                    }
                )
            }
            if (customName != null) {
                customLabelName = customName
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
