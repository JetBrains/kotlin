fun a(k: Int, b: Int = 0, c: String = "Hello World!", d: Double, vararg t: String) { println("$k $b $c $d $t") }

fun c() {
    <warning descr="SSR">a(0, 0, "This is test!", 0.0, "Test")</warning>
    <warning descr="SSR">a(0, d = 0.0, t = *<warning descr="[REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION] Redundant spread (*) operator">arrayOf("Test")</warning>)</warning>
    <warning descr="SSR">a(b = 0, c = "This is a test!", k = 0, t = *<warning descr="[REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION] Redundant spread (*) operator">arrayOf("Test")</warning>, d = 0.0)</warning>
    <warning descr="SSR">a(k = 0, d = 0.0, t = *<warning descr="[REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION] Redundant spread (*) operator">arrayOf("Test")</warning>)</warning>
}