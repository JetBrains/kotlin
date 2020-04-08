import server.*;

class Client {
    public fun foo() {
        Server().doProcessRequest()
        ServerEx().processRequest()
    }
}
