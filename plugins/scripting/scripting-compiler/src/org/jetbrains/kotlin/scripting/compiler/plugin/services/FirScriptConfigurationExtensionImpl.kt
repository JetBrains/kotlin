/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension.Factory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
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
    @Suppress("UNUSED_PARAMETER") hostConfiguration: ScriptingHostConfiguration
) : FirScriptConfiguratorExtension(session) {
    @OptIn(SymbolInternals::class)
    override fun FirScriptBuilder.configure(fileBuilder: FirFileBuilder) {

        // TODO: rewrite/extract decision logic for clarity
        val compilationConfiguration = session.scriptDefinitionProviderService?.let { providerService ->
            fileBuilder.sourceFile?.toSourceCode()?.let { script ->
                val ktFile = (script as? KtFileScriptSource)?.ktFile ?: error("only PSI scripts are supported at the moment")
                providerService.configurationProvider?.getScriptConfigurationResult(ktFile)?.valueOrNull()?.configuration
                    ?: providerService.definitionProvider?.findDefinition(script)?.compilationConfiguration
            } ?: providerService.definitionProvider?.getDefaultDefinition()?.compilationConfiguration
        }

        if (compilationConfiguration != null) {

            compilationConfiguration[ScriptCompilationConfiguration.defaultImports]?.forEach { defaultImport ->
                val trimmed = defaultImport.trim()
                val endsWithStar = trimmed.endsWith("*")
                val stripped = if (endsWithStar) trimmed.substring(0, trimmed.length - 1) else trimmed
                val fqName = FqName.fromSegments(stripped.split("."))
                fileBuilder.imports += buildImport {
                    importedFqName = fqName
                    isAllUnder = endsWithStar
                }
            }

            compilationConfiguration[ScriptCompilationConfiguration.baseClass]?.let { baseClass ->
                val baseClassFqn = FqName.fromSegments(baseClass.typeName.split("."))
                contextReceivers.add(buildContextReceiverWithFqName(baseClassFqn))

                val baseClassSymbol =
                    session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(ClassId(baseClassFqn.parent(), baseClassFqn.shortName()))
                            as? FirRegularClassSymbol
                if (baseClassSymbol != null) {
                    // assuming that if base class will be unresolved, the error will be reported on the contextReceiver
                    baseClassSymbol.fir.primaryConstructorIfAny(session)?.fir?.valueParameters?.forEach { baseCtorParameter ->
                        parameters.add(
                            buildProperty {
                                moduleData = session.moduleData
                                origin = FirDeclarationOrigin.ScriptCustomization
                                // TODO: copy type parameters?
                                returnTypeRef = baseCtorParameter.returnTypeRef
                                this.name = baseCtorParameter.name
                                this.symbol = FirPropertySymbol(this.name)
                                status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                                isLocal = true
                                isVar = false
                            }
                        )
                    }
                }
            }

            compilationConfiguration[ScriptCompilationConfiguration.implicitReceivers]?.forEach { implicitReceiver ->
                contextReceivers.add(buildContextReceiverWithFqName(FqName.fromSegments(implicitReceiver.typeName.split("."))))
            }

            compilationConfiguration[ScriptCompilationConfiguration.providedProperties]?.forEach { propertyName, propertyType ->
                val typeRef = buildUserTypeRef {
                    isMarkedNullable = propertyType.isNullable
                    propertyType.typeName.split(".").forEach {
                        qualifier.add(FirQualifierPartImpl(null, Name.identifier(it), FirTypeArgumentListImpl(null)))
                    }
                }
                parameters.add(
                    buildProperty {
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.ScriptCustomization
                        returnTypeRef = typeRef
                        this.name = Name.identifier(propertyName)
                        this.symbol = FirPropertySymbol(this.name)
                        status = FirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                        isLocal = true
                        isVar = false
                    }
                )
            }

            compilationConfiguration[ScriptCompilationConfiguration.annotationsForSamWithReceivers]?.forEach {
                _knownAnnotationsForSamWithReceiver.add(it.typeName)
            }
        }
    }

    private fun buildContextReceiverWithFqName(baseClassFqn: FqName) =
        buildContextReceiver {
            typeRef = buildUserTypeRef {
                isMarkedNullable = false
                qualifier.addAll(
                    baseClassFqn.pathSegments().map {
                        FirQualifierPartImpl(null, it, FirTypeArgumentListImpl(null))
                    }
                )
            }
        }

    private val _knownAnnotationsForSamWithReceiver = hashSetOf<String>()

    internal val knownAnnotationsForSamWithReceiver: Set<String>
        get() = _knownAnnotationsForSamWithReceiver

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirScriptConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

fun KtSourceFile.toSourceCode(): SourceCode? = when (this) {
    is KtPsiSourceFile -> (psiFile as? KtFile)?.let(::KtFileScriptSource) ?: VirtualFileScriptSource(psiFile.virtualFile)
    is KtVirtualFileSourceFile -> VirtualFileScriptSource(virtualFile)
    is KtIoFileSourceFile -> FileScriptSource(file)
    is KtInMemoryTextSourceFile -> StringScriptSource(text.toString(), name)
    else -> null
}
