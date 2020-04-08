class Test {
    fun useSamUpper(acceptor: SamAcceptor<out Number?>) {
        acceptor.acceptSam { p: Number? -> }
    }

    fun useSamLower(acceptor: SamAcceptor<in Number?>) {
        acceptor.acceptSam { p -> }
    }

    fun useSam(acceptor: SamAcceptor<*>) {
        acceptor.acceptSam { p: Any? -> }
    }
}
