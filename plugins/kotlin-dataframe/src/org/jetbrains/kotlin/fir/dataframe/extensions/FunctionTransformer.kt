package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.FirMetaContext
import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.extensions.FirFunctionTransformerExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade

class FunctionTransformer(
    session: FirSession,
    private val context: FirMetaContext,
    val refinedToOriginal: MutableMap<Name, FirBasedSymbol<*>>,
) : FirFunctionTransformerExtension(session), KotlinTypeFacade {
    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall): FirFunctionCall = with (context) {
        val (token, dataFrameSchema) =
            analyzeRefinedCallShape(call, InterpretationErrorReporter.DEFAULT) ?: return call

        call.transformCalleeReference(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                return if (element is FirResolvedNamedReference) {
                    buildResolvedNamedReference {
                        name = element.name
                        val refinedName = call.calleeReference.toResolvedCallableSymbol()?.callableId?.callableName!!
                        resolvedSymbol = refinedToOriginal[refinedName]!!
                    } as E
                } else {
                    element
                }
            }
        }, null)
        val name = token.type.classId?.shortClassName?.identifierOrNullIfSpecial!!
        val newCall = compile<FirFunctionCall>(
            """
            package org.jetbrains.kotlinx.dataframe                    

            import org.jetbrains.kotlinx.dataframe.DataFrame
        
            fun test(df1: DataFrame<*>) {
                df1.let { 
                    
                    open class _DataFrameType1
                    
                    class Scope1 {
                        val ColumnsContainer<_DataFrameType1>.col1: DataColumn<Int> get() = this["col1"] as DataColumn<Int>
                        val DataRow<_DataFrameType1>.col1: Int get() = this["col1"] as Int
                    }
                    
                    class $name : _DataFrameType1() {
                        val scope1: Scope1 = TODO()
                    }
                    
                    
                    it as DataFrame<$name>
                }
            }
            """.trimIndent(),
            listOfNotNull(((call.explicitReceiver as? FirPropertyAccessExpression)?.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir)
        )
        newCall.replaceExplicitReceiver(call.explicitReceiver)
        newCall.replaceExtensionReceiver(call.extensionReceiver)
        newCall.acceptChildren(object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
                val block = buildBlock {
                    val original = anonymousFunction.body?.statements ?: emptyList()
                    for ((i, statement) in original.withIndex()) {
                        if (i == original.lastIndex) {
                            statements.add(statement.transformSingle(InsertOriginalCall(call), null))
                        } else {
                            statements.add(statement)
                        }
                    }
                }
                block.replaceTypeRef(anonymousFunction.body?.typeRef!!)
                anonymousFunction.replaceBody(block)
            }
        })
        return newCall
    }

    private class InsertOriginalCall(val call: FirFunctionCall) :  FirDefaultTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
            return element.transformChildren(this, data) as E
        }

        override fun transformPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?): FirStatement {
            call.replaceExplicitReceiver(propertyAccessExpression)
            return call
        }
    }
}
