annotation class Anon(public val value: String) {

    public enum class E {
        A
        B
    }

    class object {

        public val field: E = E.A
    }
}

Anon("a")
trait I {
    class object {
        public val e: Anon.E = Anon.field
    }
}