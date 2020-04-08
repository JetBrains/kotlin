fun some() {
	val b = """
		|class Test() {
		|	fun test() {<caret>
		|}
		""".trimMargin()
}
//-----
fun some() {
	val b = """
		|class Test() {
		|	fun test() {
		|	<caret>
		|}
		""".trimMargin()
}