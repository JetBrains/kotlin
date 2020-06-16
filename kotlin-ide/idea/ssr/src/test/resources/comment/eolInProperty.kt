<warning descr="SSR">//
val foo1 = 1</warning>
<warning descr="SSR">val foo2 = 1 //</warning>
<warning descr="SSR">/**/
val foo3 = 1</warning>
<warning descr="SSR">val foo4 = 1 /**/</warning>
<warning descr="SSR">val foo5 /**/ = 1</warning>
/**
 *
 */
val foo6 = 1
val foo7 = 1

fun main() {
    //
    <warning descr="SSR">val bar1 = 1</warning>
    <warning descr="SSR">val bar2 = 1 //</warning>
    /**/
    <warning descr="SSR">val bar3 = 1</warning>
    <warning descr="SSR">val bar4 = 1 /**/</warning>
    <warning descr="SSR">val bar5 /**/ = 1</warning>
    /**
     *
     */
    val bar6 = 1
    val bar7 = 1

    print(bar1 + bar2 + bar3 + bar4 + bar5 + bar6 + bar7)
}