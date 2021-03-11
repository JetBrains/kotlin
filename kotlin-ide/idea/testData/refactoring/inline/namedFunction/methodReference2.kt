class C {
    fun f() {
        val value = ""
        val calculated = calculationSimple(value)
    }

    private fun calculation<caret>Simple(value: String) =
        calculationDynamic(::oneOfThePredicates, value)

    private fun calculationDynamic(predicate: (String) -> Boolean, value: String) =
        predicate(value) // much more complex IRL

    private fun oneOfThePredicates(value: String?) =
        value != null // complex decision IRL
}