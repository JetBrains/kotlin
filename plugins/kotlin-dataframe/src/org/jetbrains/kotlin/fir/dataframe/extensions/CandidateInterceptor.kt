package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.FirMetaContext
import org.jetbrains.kotlin.fir.dataframe.FlagContainer
import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.dataframe.flatten
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnKind
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class CandidateInterceptor(
    session: FirSession,
    val nextFunction: (String) -> CallableId,
    val callableState: MutableMap<Name, FirSimpleFunction>,
    val originalSymbol: MutableMap<Name, FirBasedSymbol<*>>,
    val nextName: (String) -> ClassId,
    private val context: FirMetaContext,
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
        val tokenId = nextName(suggestedName)
        val typeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                lookupTag,
                arrayOf(
                    ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(tokenId),
                        emptyArray(),
                        isNullable = false
                    )
                ),
                isNullable = false
            )
        }

        val function = buildSimpleFunctionCopy(symbol.fir) {
            name = generatedName.callableName
            body = null
            this.symbol = newSymbol
            returnTypeRef = typeRef
        }.also { newSymbol.bind(it) }
        callableState.compute(generatedName.callableName) { name, it ->
            if (it != null) error("$name is not unique")
            function
        }
        originalSymbol.compute(generatedName.callableName) { name, it ->
            if (it != null) error("$name is not unique")
            symbol
        }
        return newSymbol
    }

    companion object {
        const val rootToken = "DataFrameType1"
    }

    private var counter = AtomicInteger(0)

    private val DataRow = org.jetbrains.kotlinx.dataframe.DataRow::class.simpleName!!
    private val ColumnsContainer = org.jetbrains.kotlinx.dataframe.ColumnsContainer::class.simpleName!!
    private val DataFrame = org.jetbrains.kotlinx.dataframe.DataFrame::class.simpleName!!
    private val DataColumn = org.jetbrains.kotlinx.dataframe.DataColumn::class.simpleName!!
    private val ColumnGroup = org.jetbrains.kotlinx.dataframe.columns.ColumnGroup::class.simpleName!!

    fun renderNullability(nullable: Boolean) = if (nullable) "?" else ""

    fun renderStringLiteral(name: String) = name
        .replace("\\", "\\\\")
        .replace("$", "\\\$")
        .replace("\"", "\\\"")

    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall): FirFunctionCall = with (context) {
        val (token, dataFrameSchema) =
            analyzeRefinedCallShape(call, InterpretationErrorReporter.DEFAULT) ?: return call

        val names = linkedMapOf<SimpleCol, String>()
        val columns = dataFrameSchema.columns()

        val declarations = StringBuilder()

        fun appendSchemaDeclarations(tokenName: String, cols: List<SimpleCol>, scopeName: String) {
            declarations.appendLine("""
                abstract class $tokenName {
                    ${cols.joinToString("\n") { it.property(names) }}
                }
                
                class $scopeName {
                    ${cols.joinToString("\n") { it.extensions(tokenName, names) }}
                }
            """.trimIndent())
        }

        class SchemaDeclaration(val proposedName: String, val column: SimpleCol?, val columns: List<SimpleCol>)
        val schemas = mutableListOf<SchemaDeclaration>()

        schemas.add(SchemaDeclaration(rootToken, null, columns))

        val flatten = dataFrameSchema.flatten()
        flatten.distinctBy { it.column }.forEach {
            names[it.column] = it.path.path.last()
        }

        names.mapNotNullTo(schemas) { (column, name) ->
            when (column) {
                is SimpleColumnGroup -> {
                    SchemaDeclaration(name, column, column.columns())
                }
                is SimpleFrameColumn -> {
                    SchemaDeclaration(name, column, column.columns())
                }
                is SimpleCol -> null
                else -> error(column::class.java)
            }
        }

        val distinctBy = schemas.distinctBy { it.column }
        distinctBy.asReversed().forEachIndexed { i, declaration ->
            appendSchemaDeclarations(declaration.proposedName, declaration.columns, """Scope$i""")
        }

        val scopes = buildString {
            distinctBy.forEachIndexed { index, schemaDeclaration ->
                appendLine("""abstract val scope$index: Scope$index""")
            }
        }

        val name = token.type.classId?.shortClassName?.identifierOrNullIfSpecial!!
        declarations.appendLine("""
            abstract class $name : $rootToken() {
                $scopes
            }
        """.trimIndent())

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

        val receiver = buildString {
            append(call.explicitReceiver?.coneTypeOrNull?.classId?.asFqNameString()!!)
            if (call.explicitReceiver!!.coneTypeOrNull!!.typeArguments.isNotEmpty()) {
                if (call.explicitReceiver!!.coneTypeOrNull!!.typeArguments[0] is ConeStarProjection ||
                    (call.explicitReceiver!!.coneTypeOrNull!!.typeArguments[0].type?.toSymbol(session)!! as FirRegularClassSymbol).isLocal
                ) {
                    append("<*>")
                } else {
                    append("<")
                    append(call.explicitReceiver!!.coneTypeOrNull!!.typeArguments.joinToString { it.type?.classId?.asFqNameString()!! })
                    append(">")
                }
            }
        }

        val newCall = compile<FirFunctionCall>(
            """
            package org.jetbrains.kotlinx.dataframe                    

            import org.jetbrains.kotlinx.dataframe.*
            import org.jetbrains.kotlinx.dataframe.columns.*
        
            fun test${counter.getAndIncrement()}(df1: $receiver) {
                df1.let { 
                    $declarations
                    it as DataFrame<$name>
                }
            }
            """.trimIndent(),
            listOfNotNull(((call.explicitReceiver as? FirPropertyAccessExpression)?.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir)
        )
        newCall.replaceExplicitReceiver(call.explicitReceiver)
        newCall.replaceExtensionReceiver(call.extensionReceiver)

        val anonymousFunction = ((newCall.argumentList.arguments[0] as FirLambdaArgumentExpression).expression as FirAnonymousFunctionExpression).anonymousFunction
        val block = buildBlock {
            val original = anonymousFunction.body?.statements ?: emptyList()
            for ((i, statement) in original.withIndex()) {
                if (i == original.lastIndex) {
                    val lastStatement = (statement as FirReturnExpression).result as FirTypeOperatorCall
                    lastStatement
                        .replaceArgumentList(buildArgumentList {
                            // df.add("col") { 42 } -> it.add("col") { 42 }
                            // but is it correct to REPLACE?
                            call.replaceExplicitReceiver(lastStatement.arguments[0])
                            call.replaceExtensionReceiver(lastStatement.arguments[0])

                            arguments += call
                        })

                    statements.add(buildReturnExpression {
                        target = statement.target
                        result = lastStatement
                    })
                } else {
                    statements.add(statement)
                }
            }
        }
        block.replaceConeTypeOrNull(anonymousFunction.body?.coneTypeOrNull)
        anonymousFunction.replaceBody(block)
        return newCall
    }

    private fun SimpleCol.extensions(markerName: String, names: MutableMap<SimpleCol, String>): String {
        val getter = "this[\"${renderStringLiteral(name())}\"]"
        val name = name()
        val fieldType = when (kind()) {
            SimpleColumnKind.VALUE ->
                type.renderType()

            SimpleColumnKind.GROUP ->
                "${DataRow}<${names[this]!!}>"

            SimpleColumnKind.FRAME ->
                "${DataFrame}<${names[this]!!}>${renderNullability(type.type().isNullable)}"
        }

        val columnType = when (kind()) {
            SimpleColumnKind.VALUE ->
                "${DataColumn}<${type.renderType()}>"

            SimpleColumnKind.GROUP ->
                "${ColumnGroup}<${names[this]!!}>"

            SimpleColumnKind.FRAME -> {
                "${DataColumn}<${DataFrame}<${names[this]!!}>${renderNullability((this as SimpleFrameColumn).nullable)}>"
            }
        }

        val dfTypename = "${ColumnsContainer}<${markerName}>"
        val rowTypename = "${DataRow}<${markerName}>"

        return generatePropertyCode(
            shortMarkerName = markerName,
            typeName = dfTypename,
            name = name,
            propertyType = columnType,
            getter = getter,
        ) + "\n" +
            generatePropertyCode(
                shortMarkerName = markerName,
                typeName = rowTypename,
                name = name,
                propertyType = fieldType,
                getter = getter,
            )
    }

    fun TypeApproximation.renderType(): String {
        val type1 = type()
        val type = type1.classId!!.asFqNameString()
        val nullability = renderNullability(type1.isNullable)
        return buildString {
            append(type)
            if (type1.typeArguments.isNotEmpty()) {
                append("<")
                append(type1.typeArguments.joinToString { it.type?.classId?.asFqNameString()!! })
                append(">")
            }
            append(nullability)
        }
    }

    private fun generatePropertyCode(
        shortMarkerName: String,
        typeName: String,
        name: String,
        propertyType: String,
        getter: String,
    ): String {
        // jvm name is required to prevent signature clash like this:
        // val DataRow<Type>.name: String
        // val DataRow<Repo>.name: String
        val jvmName = "${shortMarkerName}_${name}"
        return "val $typeName.$name: $propertyType @JvmName(\"${renderStringLiteral(jvmName)}\") get() = $getter as $propertyType"
    }

    private fun SimpleCol.property(names: MutableMap<SimpleCol, String>): String {
        return when (kind()) {
            SimpleColumnKind.VALUE -> "abstract val $name: ${type.renderType()}"
            SimpleColumnKind.GROUP -> {
                val token = names[this]!!
                "abstract val $name: ${type.type().classId!!.asFqNameString()}<$token>"
            }

            SimpleColumnKind.FRAME -> {
                val token = names[this]!!
                "abstract val $name: ${type.type().classId!!.asFqNameString()}<$token>"
            }
        }
    }

}
