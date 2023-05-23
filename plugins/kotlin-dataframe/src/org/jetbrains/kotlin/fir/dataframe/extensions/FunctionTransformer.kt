package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.FirMetaContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirFunctionTransformerExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.Name

class FunctionTransformer(session: FirSession, private val context: FirMetaContext) : FirFunctionTransformerExtension(session) {
    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall): FirFunctionCall = with (context) {
        if (call.calleeReference.name == Name.identifier("add")) {
            val token = (call.typeRef as FirResolvedTypeRef).type.typeArguments[0] as ConeClassLikeType
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
            return newCall
        } else call
    }
}
