internal annotation class Anon(val value: String) {

    enum class E {
        A, B
    }

    companion object {

        val field = E.A
    }
}

@Anon("a")
internal interface I {
    companion object {
        val e: Anon.E = Anon.field
    }
}