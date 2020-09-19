// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_IGNORE

package server

public open class Server() {
    private fun <caret>doProcessRequest() = "foo"

    open fun processRequest() = doProcessRequest()
}

public class ServerEx(): Server() {
    override fun processRequest() = "foo" + doProcessRequest()
}
// DISABLE-ERRORS

