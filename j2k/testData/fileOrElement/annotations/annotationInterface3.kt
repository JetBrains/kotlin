// ERROR: Body is not allowed for annotation class
// ERROR: Modifier 'companion' is not applicable inside 'annotation class'
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