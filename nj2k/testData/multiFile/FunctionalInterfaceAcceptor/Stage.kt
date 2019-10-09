package test

class Stage {
    fun context(acceptor: Acceptor) {
        acceptor.acceptFace { p -> println(p) }
        acceptor.face = Face { p -> println(p) }
    }
}