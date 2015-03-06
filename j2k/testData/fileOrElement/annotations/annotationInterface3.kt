// ERROR: Body is not allowed for annotation class
annotation class Anon(public val value: String) {

    public enum class E {
        A
        B
    }

    default object {

        public val field: E = E.A
    }
}

Anon("a")
trait I {
    default object {
        public val e: Anon.E = Anon.field
    }
}