// LL_FIR_DIVERGENCE
// backend diagnostics are not reported in AA tests
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT


// see KT-49443
// two similar examples check dependency on declarations ordering

interface I {
    fun rename()
}

object ZipHelper {
    fun buildZip() {
        DefaultEachEntryConfiguration(0).rename()
    }
}

class DefaultEachEntryConfiguration(val entry: Int) : I {
    override fun rename() {
        entry.copy()
    }
}

fun Int.copy() = Unit

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral,
interfaceDeclaration, objectDeclaration, override, primaryConstructor, propertyDeclaration */
