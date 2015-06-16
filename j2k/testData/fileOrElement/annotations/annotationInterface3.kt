// ERROR: Body is not allowed for annotation class
// ERROR: Modifier 'companion' is not applicable inside 'annotation class'
annotation class Anon(public val value: String) {

    public enum class E {
        A, B
    }

    companion object {

        public val field: E = E.A
    }
}

Anon("a")
interface I {
    companion object {
        public val e: Anon.E = Anon.field
    }
}