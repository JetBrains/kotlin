package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlinx.dataframe.plugin.InterpretationErrorReporter
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.SchemaProperty
import org.jetbrains.kotlinx.dataframe.plugin.analyzeRefinedCallShape
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.projectOverDataColumnType
import org.jetbrains.kotlin.fir.declarations.EmptyDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.PropertyName
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupBy
import kotlin.math.abs

@OptIn(FirExtensionApiInternals::class)
class FunctionCallTransformer(
    session: FirSession,
    override val isTest: Boolean,
) : FirFunctionCallRefinementExtension(session), KotlinTypeFacade {
    companion object {
        const val DEFAULT_NAME = "DataFrameType"
    }

    private interface CallTransformer {
        fun interceptOrNull(callInfo: CallInfo, symbol: FirNamedFunctionSymbol, hash: String): CallReturnType?

        /**
         * must still generate let with declared class from interceptOrNull when interpretation fails.
         * it should only return null if later some frontend checker fails compilation in general
         */
        fun transformOrNull(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall?
    }

    // also update [ReturnTypeBasedReceiverInjector.SCHEMA_TYPES]
    private val transformers = listOf(
        GroupByCallTransformer(),
        DataFrameCallTransformer(),
        DataRowCallTransformer(),
        ColumnGroupCallTransformer(),
    )

    override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
        val callSiteAnnotations = (callInfo.callSite as? FirAnnotationContainer)?.annotations ?: emptyList()
        if (callSiteAnnotations.any { it.fqName(session)?.shortName()?.equals(Name.identifier("DisableInterpretation")) == true }) {
            return null
        }
        val noRefineAnnotation =
            symbol.resolvedAnnotationClassIds.none { it.shortClassName == Name.identifier("Refine") }
        val optIn = symbol.resolvedAnnotationClassIds.any { it.shortClassName == Name.identifier("OptInRefine") } &&
                callSiteAnnotations.any { it.fqName(session)?.shortName()?.equals(Name.identifier("Import")) == true }
        if (noRefineAnnotation && !optIn) {
            return null
        }

        val hash = run {
            val hash = callInfo.name.hashCode() + callInfo.arguments.sumOf {
                when (it) {
                    is FirLiteralExpression -> it.value.hashCode()
                    else -> it.source?.text?.hashCode() ?: 42
                }
            }
            hashToTwoCharString(abs(hash))
        }

        return transformers.firstNotNullOfOrNull { it.interceptOrNull(callInfo, symbol, hash) }
    }

    private fun hashToTwoCharString(hash: Int): String {
        val baseChars = "0123456789"
        val base = baseChars.length
        val positiveHash = abs(hash)
        val char1 = baseChars[positiveHash % base]
        val char2 = baseChars[(positiveHash / base) % base]

        return "$char1$char2"
    }

    override fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall {
        return transformers
            .firstNotNullOfOrNull { it.transformOrNull(call, originalSymbol) }
            ?: call
    }

    override fun ownsSymbol(symbol: FirRegularClassSymbol): Boolean {
        return symbol.anchor != null
    }

    override fun anchorElement(symbol: FirRegularClassSymbol): KtSourceElement {
        return symbol.anchor!!
    }

    override fun restoreSymbol(call: FirFunctionCall, name: Name): FirRegularClassSymbol? {
        val newType = (call.resolvedType.typeArguments.getOrNull(0) as? ConeClassLikeType)?.toRegularClassSymbol(session)
        return newType?.generatedClasses?.get(name)
    }

    inner class DataSchemaLikeCallTransformer(val classId: ClassId) : CallTransformer {
        override fun interceptOrNull(callInfo: CallInfo, symbol: FirNamedFunctionSymbol, hash: String): CallReturnType? {
            if (symbol.resolvedReturnType.fullyExpandedClassId(session) != classId) return null
            // possibly null if explicit receiver type is typealias
            val argument = (callInfo.explicitReceiver?.resolvedType)?.typeArguments?.getOrNull(0)
            val newDataFrameArgument = buildNewTypeArgument(argument, callInfo.name, hash, callInfo.callSite)

            val lookupTag = ConeClassLikeLookupTagImpl(classId)
            val typeRef = buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    lookupTag,
                    arrayOf(
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagWithFixedSymbol(newDataFrameArgument.classId, newDataFrameArgument.symbol),
                            emptyArray(),
                            isMarkedNullable = false
                        )
                    ),
                    isMarkedNullable = false
                )
            }
            return CallReturnType(typeRef)
        }

        @OptIn(SymbolInternals::class)
        override fun transformOrNull(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall? {
            val callResult = analyzeRefinedCallShape<PluginDataFrameSchema>(call, classId, InterpretationErrorReporter.DEFAULT)
            val (tokens, dataFrameSchema) = callResult ?: return null
            val token = tokens[0]
            val firstSchema = token.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!.toRegularClassSymbol(session)?.fir!!
            val dataSchemaApis = materialize(dataFrameSchema ?: PluginDataFrameSchema.EMPTY, call, firstSchema)

            val tokenFir = token.toRegularClassSymbol(session)!!.fir
            tokenFir.callShapeData = CallShapeData.RefinedType(dataSchemaApis.map { it.scope.symbol })

            return buildScopeFunctionCall(call, originalSymbol, dataSchemaApis, listOf(tokenFir)) { tokenFir.generatedClasses = it }
        }
    }

    inner class DataFrameCallTransformer : CallTransformer by DataSchemaLikeCallTransformer(Names.DF_CLASS_ID)

    inner class DataRowCallTransformer : CallTransformer by DataSchemaLikeCallTransformer(Names.DATA_ROW_CLASS_ID)

    inner class ColumnGroupCallTransformer : CallTransformer by DataSchemaLikeCallTransformer(Names.COLUM_GROUP_CLASS_ID)

    inner class GroupByCallTransformer : CallTransformer {
        override fun interceptOrNull(
            callInfo: CallInfo,
            symbol: FirNamedFunctionSymbol,
            hash: String,
        ): CallReturnType? {
            if (symbol.resolvedReturnType.fullyExpandedClassId(session) != Names.GROUP_BY_CLASS_ID) return null
            val keys = buildNewTypeArgument(null, Name.identifier("Key"), hash, callInfo.callSite)
            val group = buildNewTypeArgument(null, Name.identifier("Group"), hash, callInfo.callSite)
            val lookupTag = ConeClassLikeLookupTagImpl(Names.GROUP_BY_CLASS_ID)
            val typeRef = buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    lookupTag,
                    arrayOf(
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagWithFixedSymbol(keys.classId, keys.symbol),
                            emptyArray<ConeTypeProjection>(),
                            isMarkedNullable = false
                        ),
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagWithFixedSymbol(group.classId, group.symbol),
                            emptyArray<ConeTypeProjection>(),
                            isMarkedNullable = false
                        )
                    ),
                    isMarkedNullable = false
                )
            }
            return CallReturnType(typeRef)
        }

        @OptIn(SymbolInternals::class)
        override fun transformOrNull(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall? {
            val callResult = analyzeRefinedCallShape<GroupBy>(call, Names.GROUP_BY_CLASS_ID, InterpretationErrorReporter.DEFAULT)
            val (rootMarkers, groupBy) = callResult ?: return null

            val keyMarker = rootMarkers[0]
            val groupMarker = rootMarkers[1]

            val (keySchema, groupSchema) = if (groupBy != null) {
                val keySchema = groupBy.keys
                val groupSchema = groupBy.groups
                keySchema to groupSchema
            } else {
                PluginDataFrameSchema.EMPTY to PluginDataFrameSchema.EMPTY
            }

            val firstSchema = keyMarker.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!.toRegularClassSymbol(session)?.fir!!
            val firstSchema1 = groupMarker.toClassSymbol(session)?.resolvedSuperTypes?.get(0)!!.toRegularClassSymbol(session)?.fir!!

            val keyApis = materialize(keySchema, call, firstSchema, "Key")
            val groupApis = materialize(groupSchema, call, firstSchema1, "Group", i = keyApis.size)

            val groupToken = keyMarker.toRegularClassSymbol(session)!!.fir
            groupToken.callShapeData = CallShapeData.RefinedType(keyApis.map { it.scope.symbol })

            val keyToken = groupMarker.toRegularClassSymbol(session)!!.fir
            keyToken.callShapeData = CallShapeData.RefinedType(groupApis.map { it.scope.symbol })

            return buildScopeFunctionCall(
                call,
                originalSymbol,
                keyApis + groupApis,
                additionalDeclarations = listOf(groupToken, keyToken)
            ) {
                keyToken.generatedClasses = it
            }
        }
    }

    private fun buildNewTypeArgument(argument: ConeTypeProjection?, name: Name, hash: String, callSite: FirElement): FirRegularClass {
        val suggestedName = if (argument == null) {
            "${name.asTokenName()}_$hash"
        } else {
            when (argument) {
                is ConeStarProjection -> {
                    "${name.asTokenName()}_$hash"
                }
                is ConeKotlinTypeProjection -> {
                    val titleCase = argument.type.classId?.shortClassName
                        ?.identifierOrNullIfSpecial?.titleCase()
                        ?.substringBeforeLast("_")
                        ?: DEFAULT_NAME
                    "${titleCase}_$hash"
                }
            }
        }
        val tokenId = nextName("${suggestedName}I")
        val token = buildSchema(tokenId)

        val dataFrameTypeId = nextName(suggestedName)
        val dataFrameType = buildRegularClass {
            moduleData = session.moduleData
            source = callSite.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.ABSTRACT, EffectiveVisibility.Local)
            deprecationsProvider = EmptyDeprecationsProvider
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            superTypeRefs += buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagWithFixedSymbol(tokenId, token.symbol),
                    emptyArray(),
                    isMarkedNullable = false
                )
            }

            this.name = dataFrameTypeId.shortClassName
            this.symbol = FirRegularClassSymbol(dataFrameTypeId)
        }
        return dataFrameType
    }

    private fun nextName(s: String) = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName(s), true)

    private fun Name.asTokenName() = identifierOrNullIfSpecial?.titleCase() ?: DEFAULT_NAME

    @OptIn(SymbolInternals::class)
    private fun buildScopeFunctionCall(
        call: FirFunctionCall,
        originalSymbol: FirNamedFunctionSymbol,
        dataSchemaApis: List<DataSchemaApi>,
        additionalDeclarations: List<FirRegularClass>,
        saveGeneratedClasses: (Map<Name, FirRegularClassSymbol>) -> Unit,
    ): FirFunctionCall {

        val explicitReceiver = call.explicitReceiver
        val receiverType = explicitReceiver?.resolvedType
        val returnType = call.resolvedType
        val scopeFunction = if (explicitReceiver != null) findLet() else findRun()
        val originalSource = call.calleeReference.source

        // original call is inserted later
        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    @Suppress("UNCHECKED_CAST")
                    buildResolvedNamedReference {
                        this.name = element.name
                        resolvedSymbol = originalSymbol
                    } as E
                } else {
                    element
                }
            }
        }, null)

        val callExplicitReceiver = call.explicitReceiver
        val callDispatchReceiver = call.dispatchReceiver
        val callExtensionReceiver = call.extensionReceiver

        val allLocalClasses = buildList {
            dataSchemaApis.asReversed().forEach {
                this += it.schema
                this += it.scope
            }
            addAll(additionalDeclarations)
        }
        allLocalClasses.forEach {
            it.anchor = call.source
        }
        saveGeneratedClasses(allLocalClasses.associate { it.name to it.symbol })

        val argument = buildAnonymousFunctionExpression {
            isTrailingLambda = true
            val fSymbol = FirAnonymousFunctionSymbol()
            val target = FirFunctionTarget(null, isLambda = true)
            anonymousFunction = buildAnonymousFunction {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                deprecationsProvider = EmptyDeprecationsProvider
                returnTypeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                val parameterSymbol = receiverType?.let {
                    val itName = Name.identifier("it")
                    val parameterSymbol = FirValueParameterSymbol()
                    valueParameters += buildValueParameter {
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                        returnTypeRef = buildResolvedTypeRef {
                            coneType = receiverType
                        }
                        this.name = itName
                        this.symbol = parameterSymbol
                        containingDeclarationSymbol = fSymbol
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                    parameterSymbol
                }
                body = buildBlock {
                    this.coneTypeOrNull = returnType
                    statements += allLocalClasses
                    statements += buildReturnExpression {
                        if (parameterSymbol != null) {
                            val itPropertyAccess = buildPropertyAccessExpression {
                                coneTypeOrNull = receiverType
                                calleeReference = buildResolvedNamedReference {
                                    name = parameterSymbol.name
                                    resolvedSymbol = parameterSymbol
                                }
                            }
                            if (callDispatchReceiver != null) {
                                call.replaceDispatchReceiver(itPropertyAccess)
                            }
                            call.replaceExplicitReceiver(itPropertyAccess)
                            if (callExtensionReceiver != null) {
                                call.replaceExtensionReceiver(itPropertyAccess)
                            }
                        }

                        result = call
                        this.target = target
                    }
                }
                this.symbol = fSymbol
                isLambda = true
                hasExplicitParameterList = false
                typeRef = buildResolvedTypeRef {
                    coneType = if (receiverType != null) {
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function1"))),
                            typeArguments = arrayOf(receiverType, returnType),
                            isMarkedNullable = false
                        )
                    } else {
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin"), Name.identifier("Function0"))),
                            typeArguments = arrayOf(returnType),
                            isMarkedNullable = false
                        )
                    }
                }
                invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                inlineStatus = InlineStatus.Inline
            }.also {
                target.bind(it)
            }
        }

        val newCall1 = buildFunctionCall {
            // source = call.source makes IDE navigate to `let` declaration
            source = null
            this.coneTypeOrNull = returnType
            if (receiverType != null) {
                typeArguments += buildTypeProjectionWithVariance {
                    typeRef = buildResolvedTypeRef {
                        coneType = receiverType
                    }
                    variance = Variance.INVARIANT
                }
            }

            typeArguments += buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef {
                    coneType = returnType
                }
                variance = Variance.INVARIANT
            }
            dispatchReceiver = null
            this.explicitReceiver = callExplicitReceiver
            extensionReceiver = callExtensionReceiver ?: callDispatchReceiver
            argumentList = buildResolvedArgumentList(
                original = null,
                linkedMapOf(argument to scopeFunction.valueParameterSymbols[0].fir)
            )
            calleeReference = buildResolvedNamedReference {
                source = originalSource
                this.name = scopeFunction.name
                resolvedSymbol = scopeFunction
            }
        }
        return newCall1
    }

    private fun materialize(
        dataFrameSchema: PluginDataFrameSchema,
        call: FirFunctionCall,
        firstSchema: FirRegularClass,
        prefix: String = "",
        i: Int = 0,
    ): List<DataSchemaApi> {
        var i = i
        val dataSchemaApis = mutableListOf<DataSchemaApi>()
        val usedNames = mutableMapOf<String, Int>()
        fun PluginDataFrameSchema.materialize(
            schema: FirRegularClass? = null,
            suggestedName: String? = null,
        ): DataSchemaApi {
            val schema = if (schema != null) {
                schema
            } else {
                requireNotNull(suggestedName)
                val uniqueSuffix = usedNames.compute(suggestedName) { _, i -> (i ?: 0) + 1 }
                val name = nextName(suggestedName + uniqueSuffix)
                buildSchema(name)
            }

            val scopeId = ClassId(CallableId.PACKAGE_FQ_NAME_FOR_LOCAL, FqName("Scope${i++}"), true)
            val scope = buildRegularClass {
                moduleData = session.moduleData
                source = call.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
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
                    val text = call.source?.text ?: call.calleeReference.name
                    val name =
                        "${column.name.titleCase()}_${hashToTwoCharString(abs(text.hashCode()))}"
                    return materialize(suggestedName = "$prefix$name")
                }

                when (it) {
                    is SimpleColumnGroup -> {
                        val nestedSchema = PluginDataFrameSchema(it.columns()).materialize(it)
                        val columnsContainerReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.COLUM_GROUP_CLASS_ID),
                                typeArguments = arrayOf(nestedSchema.schema.defaultType()),
                                isMarkedNullable = false
                            )

                        val dataRowReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                                typeArguments = arrayOf(nestedSchema.schema.defaultType()),
                                isMarkedNullable = false
                            )

                        SchemaProperty(schema.defaultType(), PropertyName.of(it.name), dataRowReturnType, columnsContainerReturnType)
                    }

                    is SimpleFrameColumn -> {
                        val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize(it)
                        val frameColumnReturnType =
                            ConeClassLikeTypeImpl(
                                ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
                                typeArguments = arrayOf(nestedClassMarker.schema.defaultType()),
                                isMarkedNullable = false
                            )

                        SchemaProperty(
                            marker = schema.defaultType(),
                            propertyName = PropertyName.of(it.name),
                            dataRowReturnType = frameColumnReturnType,
                            columnContainerReturnType = frameColumnReturnType.toFirResolvedTypeRef()
                                .projectOverDataColumnType()
                        )
                    }

                    is SimpleDataColumn -> SchemaProperty(
                        marker = schema.defaultType(),
                        propertyName = PropertyName.of(it.name),
                        dataRowReturnType = it.type.type(),
                        columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
                    )
                }
            }
            schema.callShapeData = CallShapeData.Schema(properties)
            scope.callShapeData = CallShapeData.Scope(properties, call.calleeReference.source)
            val schemaApi = DataSchemaApi(schema, scope)
            dataSchemaApis.add(schemaApi)
            return schemaApi
        }

        dataFrameSchema.materialize(firstSchema)
        return dataSchemaApis
    }

    data class DataSchemaApi(val schema: FirRegularClass, val scope: FirRegularClass)

    private fun buildSchema(tokenId: ClassId): FirRegularClass {
        val token = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
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
        return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("let")).first()
    }

    private fun findRun(): FirFunctionSymbol<*> {
        return session.symbolProvider.getTopLevelFunctionSymbols(FqName("kotlin"), Name.identifier("run"))
            .first { it.typeParameterSymbols.size == 1 }
    }

    private fun String.titleCase() = replaceFirstChar { it.uppercaseChar() }
}
