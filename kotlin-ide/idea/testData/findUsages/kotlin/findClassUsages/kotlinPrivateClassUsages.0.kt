// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
package server

public open class Server() {
    private class <caret>Foo {

    }

    open fun processRequest() = Foo()
}

public class ServerEx(): Server() {
    override fun processRequest() = Server.Foo()
}

// DISABLE-ERRORS