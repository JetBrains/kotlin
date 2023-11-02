package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.CallShapeData
import org.jetbrains.kotlin.fir.dataframe.FlagContainer
import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.dataframe.callShapeData
import org.jetbrains.kotlin.fir.dataframe.flatten
import org.jetbrains.kotlin.fir.dataframe.projectOverDataColumnType
import org.jetbrains.kotlin.fir.declarations.EmptyDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLambdaArgumentExpression
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
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
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import kotlin.math.abs

class NewCandidateInterceptor(
    session: FirSession,
    val nextFunction: (String) -> CallableId,
    val nextName: (String) -> ClassId,
    val refinedToOriginal: MutableMap<Name, FirBasedSymbol<*>>,
    val flag: FlagContainer,
) : FirFunctionCallRefinementExtension(session), KotlinTypeFacade {
    val Key = DataFramePlugin

    @OptIn(SymbolInternals::class)
    override fun intercept(callInfo: CallInfo, symbol: FirBasedSymbol<*>): FirBasedSymbol<*>? {
        if (!flag.shouldIntercept) return null
        val callSiteAnnotations = (callInfo.callSite as? FirAnnotationContainer)?.annotations ?: emptyList()
        if (callSiteAnnotations.any { it.fqName(session)?.shortName()?.equals(Name.identifier("DisableInterpretation")) == true }) {
            return null
        }
        if (symbol.annotations.none { it.fqName(session)?.shortName()?.equals(Name.identifier("Refine")) == true }) {
            return null
        }
        if (symbol !is FirNamedFunctionSymbol) return null
        val lookupTag = ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID)
        var hash = callInfo.name.hashCode() + callInfo.arguments.sumOf {
            when (it) {
                is FirConstExpression<*> -> it.value.hashCode()
                else -> 42
            }
        }
        hash = abs(hash)
        val generatedName = nextFunction("${symbol.name.identifier}_${hash + 1}")

        val newSymbol = FirNamedFunctionSymbol(generatedName)

        // possibly null if explicit receiver type is AnyFrame
        val argument = (callInfo.explicitReceiver?.coneTypeOrNull)?.typeArguments?.singleOrNull()
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

        buildSimpleFunctionCopy(symbol.fir) {
            name = generatedName.callableName
            body = null
            this.symbol = newSymbol
            returnTypeRef = typeRef
        }.also { newSymbol.bind(it) }

        refinedToOriginal.compute(generatedName.callableName) { name, it ->
            if (it != null) error("$name is not unique")
            symbol
        }
        return newSymbol
    }

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall): FirFunctionCall {
        val (token, dataFrameSchema) =
            analyzeRefinedCallShape(call, InterpretationErrorReporter.DEFAULT) ?: return call

        val explicitReceiver = call.explicitReceiver ?: return call
        val receiverType = explicitReceiver.coneTypeOrNull ?: return call
        val returnType = call.coneTypeOrNull ?: return call

        val names = linkedMapOf<SimpleCol, String>()
        val columns = dataFrameSchema.columns()

        val resolvedLet = findLet()
        val parameter = resolvedLet.valueParameterSymbols[0]

        class SchemaDeclaration(/*val proposedName: ClassId, */val column: SimpleCol?, val columns: List<SimpleCol>)
        val schemas = mutableListOf<SchemaDeclaration>()

        val rootToken = token.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!
        schemas.add(SchemaDeclaration(/*rootToken.classId, */null, columns))

        val flatten = dataFrameSchema.flatten()
        flatten.distinctBy { it.column }.forEach {
            names[it.column] = it.path.path.last()
        }

        names.mapNotNullTo(schemas) { (column, name) ->
            when (column) {
                is SimpleColumnGroup -> {
                    SchemaDeclaration(/*ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier(name)),*/ column, column.columns())
                }
                is SimpleFrameColumn -> {
                    SchemaDeclaration(/*ClassId(FqName("org.jetbrains.kotlinx.dataframe"), Name.identifier(name)),*/ column, column.columns())
                }
                is SimpleCol -> null
                else -> error(column::class.java)
            }
        }

        val distinctBy = schemas.distinctBy { it.column }


        val root = distinctBy[0]
        val rest = distinctBy.drop(1)

        val rootClass = token.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!.toRegularClassSymbol(session)?.fir!!

        val properties = root.columns.map {
            SchemaProperty(
                marker = rootToken,
                name = it.name,
                dataRowReturnType = it.type.type(),
                columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
            )
        }


        // original call is inserted later
        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    buildResolvedNamedReference {
                        this.name = element.name
                        val refinedName = call.calleeReference.toResolvedCallableSymbol()?.callableId?.callableName!!
                        resolvedSymbol = refinedToOriginal[refinedName]!!
                    } as E
                } else {
                    element
                }
            }
        }, null)

        rootClass.callShapeData = CallShapeData.Schema(properties)
        val scopeId = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), FqName("Scope1"), true)
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
        scope.callShapeData = CallShapeData.Scope(rootClass.symbol, properties)


        val tokenFir = token.toClassSymbol(session)!!.fir
        tokenFir.callShapeData = CallShapeData.RefinedType(listOf(scope.symbol))

        val argument = buildLambdaArgumentExpression {
            expression = buildAnonymousFunctionExpression {
                val fSymbol = FirAnonymousFunctionSymbol()
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
                        symbol = parameterSymbol
                        containingFunctionSymbol = fSymbol
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }.also { parameterSymbol.bind(it) }
                    body = buildBlock {
                        this.coneTypeOrNull = returnType

                        statements += rootClass

                        statements += scope

                        statements += tokenFir
                    }
                    symbol = fSymbol
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
                }.also { fSymbol.bind(it) }
            }
        }

        val newCall1 = buildFunctionCall {
            this.coneTypeOrNull = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(call.coneTypeOrNull?.classId!!),
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLookupTagWithFixedSymbol(tokenFir.symbol.classId, tokenFir.symbol),
                        emptyArray(),
                        isNullable = false
                    )
                ),
                isNullable = false
            )
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
            dispatchReceiver = call.dispatchReceiver
            this.explicitReceiver = call.explicitReceiver
            extensionReceiver = call.extensionReceiver
            argumentList = buildResolvedArgumentList(linkedMapOf(argument to parameter.fir))
            calleeReference = buildResolvedNamedReference {
                this.name = Name.identifier("let")
                resolvedSymbol = resolvedLet
            }
        }
        return newCall1
    }

    private fun findLet(): FirFunctionSymbol<*> {
        return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).single()
    }
}
