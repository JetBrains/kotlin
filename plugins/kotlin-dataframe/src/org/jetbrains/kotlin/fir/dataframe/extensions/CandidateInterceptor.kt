package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.cli.common.repl.replEscapeLineBreaks
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.CallShapeData
import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.dataframe.callShapeData
import org.jetbrains.kotlin.fir.dataframe.projectOverDataColumnType
import org.jetbrains.kotlin.fir.declarations.EmptyDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toClassSymbol
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import kotlin.math.abs

class CandidateInterceptor(
    val path: String?,
    session: FirSession,
    val nextFunction: (String) -> CallableId,
    val nextName: (String) -> ClassId,
) : FirFunctionCallRefinementExtension(session), KotlinTypeFacade {
    val Key = DataFramePlugin

    override val resolutionPath: String? = path

    override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
        val callSiteAnnotations = (callInfo.callSite as? FirAnnotationContainer)?.annotations ?: emptyList()
        if (callSiteAnnotations.any { it.fqName(session)?.shortName()?.equals(Name.identifier("DisableInterpretation")) == true }) {
            return null
        }
        if (symbol.annotations.none { it.fqName(session)?.shortName()?.equals(Name.identifier("Refine")) == true }) {
            return null
        }
        val lookupTag = ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID)
        var hash = callInfo.name.hashCode() + callInfo.arguments.sumOf {
            when (it) {
                is FirLiteralExpression<*> -> it.value.hashCode()
                else -> 42
            }
        }
        hash = abs(hash)
        val generatedName = nextFunction("${symbol.name.identifier}_${hash + 1}")

        val newSymbol = FirNamedFunctionSymbol(generatedName)

        // possibly null if explicit receiver type is AnyFrame
        val argument = (callInfo.explicitReceiver?.resolvedType)?.typeArguments?.singleOrNull()
        val suggestedName = if (argument == null) {
            "${callInfo.name.identifier.titleCase()}_$hash"
        } else {
            when (argument) {
                is ConeStarProjection -> "${callInfo.name.identifier.titleCase()}_$hash"
                is ConeKotlinTypeProjection -> {
                    val titleCase = argument.type.classId?.shortClassName?.identifier?.titleCase()?.substringBeforeLast("_")
                    "${titleCase}_$hash"
                }
            }
        }
        val tokenId = nextName("${suggestedName}I")

        val token = buildSchema(tokenId)

        val dataFrameTypeId = nextName(suggestedName)
        val dataFrameType = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Source
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            superTypeRefs += buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    ConeClassLookupTagWithFixedSymbol(tokenId, token.symbol),
                    emptyArray(),
                    isNullable = false
                )
            }

            name = dataFrameTypeId.shortClassName
            this.symbol = FirRegularClassSymbol(dataFrameTypeId)
        }

        val typeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                lookupTag,
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLookupTagWithFixedSymbol(dataFrameTypeId, dataFrameType.symbol),
                        emptyArray(),
                        isNullable = false
                    )
                ),
                isNullable = false
            )
        }
        return CallReturnType(typeRef)
    }

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall {
        val (token, dataFrameSchema) =
            analyzeRefinedCallShape(call, InterpretationErrorReporter.DEFAULT) ?: return call

        val explicitReceiver = call.explicitReceiver ?: return call
        val receiverType = explicitReceiver.resolvedType
        val returnType = call.resolvedType

        val resolvedLet = findLet()
        val parameter = resolvedLet.valueParameterSymbols[0]


        val firstSchema = token.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!.toRegularClassSymbol(session)?.fir!!

        data class DataSchemaApi(val schema: FirRegularClass, val scope: FirRegularClass) {
            val scopeCallShapeData get() = scope.callShapeData
            val schemaCallShapeData get() = schema.callShapeData
        }

        var i = 0
        val dataSchemaApis = mutableListOf<DataSchemaApi>()
        fun PluginDataFrameSchema.materialize(schema: FirRegularClass? = null, suggestedName: String? = null): DataSchemaApi {
            val schema = if (schema != null) {
                schema
            } else {
                requireNotNull(suggestedName)
                val name = nextName(suggestedName)
                buildSchema(name)
            }

            val scopeId = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName("Scope${i++}"), true)
            val scope = buildRegularClass {
                moduleData = session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Source
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                deprecationsProvider = EmptyDeprecationsProvider
                classKind = ClassKind.CLASS
                scopeProvider = FirKotlinScopeProvider()
                superTypeRefs += FirImplicitAnyTypeRef(null)

                this.name = scopeId.shortClassName
                this.symbol = FirRegularClassSymbol(scopeId)
            }

            val properties = columns().map {
                fun PluginDataFrameSchema.materialize(column: SimpleCol): DataSchemaApi {
                    // TODO
                    val name = "${column.name.titleCase().replEscapeLineBreaks()}_${abs(call.calleeReference.name.hashCode())}"
                    return materialize(suggestedName = name)
                }

                when (it) {
                    is SimpleColumnGroup -> {
                        val nestedSchema = PluginDataFrameSchema(it.columns()).materialize(it)
                        val columnsContainerReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.COLUM_GROUP_CLASS_ID),
                                typeArguments = arrayOf(nestedSchema.schema.defaultType()),
                                isNullable = false
                            )

                        val dataRowReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                                typeArguments = arrayOf(nestedSchema.schema.defaultType()),
                                isNullable = false
                            )

                        SchemaProperty(schema.defaultType(), it.name, dataRowReturnType, columnsContainerReturnType)
                    }

                    is SimpleFrameColumn -> {
                        val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize(it)
                        val frameColumnReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
                                typeArguments = arrayOf(nestedClassMarker.schema.defaultType()),
                                isNullable = it.nullable
                            )

                        SchemaProperty(
                            marker = schema.defaultType(),
                            name = it.name,
                            dataRowReturnType = frameColumnReturnType,
                            columnContainerReturnType = frameColumnReturnType.toFirResolvedTypeRef().projectOverDataColumnType()
                        )
                    }

                    is SimpleCol -> SchemaProperty(
                        marker = schema.defaultType(),
                        name = it.name,
                        dataRowReturnType = it.type.type(),
                        columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
                    )

                    else -> TODO("shouldn't happen")
                }
            }
            schema.callShapeData = CallShapeData.Schema(properties)
            scope.callShapeData = CallShapeData.Scope(schema.symbol, properties)
            val schemaApi = DataSchemaApi(schema, scope)
            dataSchemaApis.add(schemaApi)
            return schemaApi
        }

        dataFrameSchema.materialize(firstSchema)

        // original call is inserted later
        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    buildResolvedNamedReference {
                        this.name = element.name
                        resolvedSymbol = originalSymbol
                    } as E
                } else {
                    element
                }
            }
        }, null)

        val tokenFir = token.toClassSymbol(session)!!.fir
        tokenFir.callShapeData = CallShapeData.RefinedType(dataSchemaApis.map { it.scope.symbol })

        val callExplicitReceiver = call.explicitReceiver
        val callDispatchReceiver = call.dispatchReceiver
        val callExtensionReceiver = call.extensionReceiver

        val argument = buildLambdaArgumentExpression {
            expression = buildAnonymousFunctionExpression {
                val fSymbol = FirAnonymousFunctionSymbol()
                val target = FirFunctionTarget(null, isLambda = true)
                anonymousFunction = buildAnonymousFunction {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Source
                    status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                    deprecationsProvider = EmptyDeprecationsProvider
                    returnTypeRef = buildResolvedTypeRef {
                        type = returnType
                    }
                    val itName = Name.identifier("it")
                    val parameterSymbol = FirValueParameterSymbol(itName)
                    valueParameters += buildValueParameter {
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = buildResolvedTypeRef {
                            type = receiverType
                        }
                        this.name = itName
                        this.symbol = parameterSymbol
                        containingFunctionSymbol = fSymbol
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }.also { parameterSymbol.bind(it) }
                    body = buildBlock {
                        this.coneTypeOrNull = returnType
                        dataSchemaApis.asReversed().forEach {
                            statements += it.schema
                            statements += it.scope
                        }

                        statements += tokenFir

                        statements += buildReturnExpression {
                            val itPropertyAccess = buildPropertyAccessExpression {
                                coneTypeOrNull = receiverType
                                calleeReference = buildResolvedNamedReference {
                                    name = itName
                                    resolvedSymbol = parameterSymbol
                                }
                            }
                            call.replaceExplicitReceiver(itPropertyAccess)
                            call.replaceExtensionReceiver(itPropertyAccess)
                            result = call
                            this.target = target
                        }
                    }
                    this.symbol = fSymbol
                    isLambda = true
                    hasExplicitParameterList = false
                    typeRef = buildResolvedTypeRef {
                        type = ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function1"))),
                            typeArguments = arrayOf(receiverType, returnType),
                            isNullable = false
                        )
                    }
                    invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                    inlineStatus = InlineStatus.Inline
                }.also {
                    target.bind(it)
                    fSymbol.bind(it)
                }
            }
        }

        val newCall1 = buildFunctionCall {
            source = call.source
            this.coneTypeOrNull = returnType
            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    type = receiverType
                }
                variance = Variance.INVARIANT
            }

            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    type = returnType
                }
                variance = Variance.INVARIANT
            }
            dispatchReceiver = callDispatchReceiver
            this.explicitReceiver = callExplicitReceiver
            extensionReceiver = callExtensionReceiver
            argumentList = buildResolvedArgumentList(linkedMapOf(argument to parameter.fir))
            calleeReference = buildResolvedNamedReference {
                source = call.calleeReference.source
                this.name = Name.identifier("let")
                resolvedSymbol = resolvedLet
            }
        }
        return newCall1
    }

    private fun buildSchema(tokenId: ClassId): FirRegularClass {
        val token = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Source
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            superTypeRefs += FirImplicitAnyTypeRef(null)

            name = tokenId.shortClassName
            this.symbol = FirRegularClassSymbol(tokenId)
        }
        return token
    }

    private fun findLet(): FirFunctionSymbol<*> {
        return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
    }

    private fun String.titleCase() = replaceFirstChar { it.uppercaseChar() }
}
