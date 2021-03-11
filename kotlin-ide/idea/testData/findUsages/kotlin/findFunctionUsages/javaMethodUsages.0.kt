// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package testing

public open class Server() {
    public open fun <caret>processRequest() = "foo"
}

public class ServerEx(): Server() {
    public override fun processRequest() = "foofoo"
}
