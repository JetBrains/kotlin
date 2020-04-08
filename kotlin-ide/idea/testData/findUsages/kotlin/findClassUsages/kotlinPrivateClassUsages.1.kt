import server.*;

class Client {
    public fun foo() {
        println(Server.Foo())
        ServerEx().processRequest()
    }
}
