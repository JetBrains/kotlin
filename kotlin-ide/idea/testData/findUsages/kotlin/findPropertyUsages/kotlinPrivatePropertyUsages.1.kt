import server.*;

class Client {
    public fun foo() {
        println(Server().foo)
        ServerEx().processRequest()
    }
}
