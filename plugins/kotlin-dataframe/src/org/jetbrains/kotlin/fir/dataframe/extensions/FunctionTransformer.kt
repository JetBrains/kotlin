package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.FirMetaContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirFunctionTransformerExtension
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.Name

class FunctionTransformer(session: FirSession, private val context: FirMetaContext) : FirFunctionTransformerExtension(session) {
    @OptIn(SymbolInternals::class)
    override fun transform(call: FirFunctionCall): FirFunctionCall = with (context) {
        if (call.calleeReference.name == Name.identifier("add")) {
            val newCall = compile<FirFunctionCall>(
                """
                import org.jetbrains.kotlinx.dataframe.DataFrame
            
                fun test(df1: DataFrame<*>) {
                    df1.let { println(it); it }
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
