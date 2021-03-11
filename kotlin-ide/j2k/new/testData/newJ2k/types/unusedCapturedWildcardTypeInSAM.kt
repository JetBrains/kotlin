class Test {
    fun useSamUpper(acceptor: SamAcceptor<out Number?>) {
        acceptor.acceptSam { p: Int -> }
    }

    fun useSamLower(acceptor: SamAcceptor<in Number?>) {
        acceptor.acceptSam { p: Int -> }
    }

    fun useSam(acceptor: SamAcceptor<*>) {
        acceptor.acceptSam { p: Int -> }
    }
}
