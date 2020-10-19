<warning descr="SSR">//</warning>
val foo1 = 1
val foo2 = 1 <warning descr="SSR">//</warning>
<warning descr="SSR">/**/</warning>
val foo3 = 1
val foo4 = 1 <warning descr="SSR">/**/</warning>
val foo5 <warning descr="SSR">/**/</warning> = 1
<warning descr="SSR">/**
 *
 */</warning>
val foo6 = 1
val foo7 = 1

fun main() {
    <warning descr="SSR">//</warning>
    val bar1 = 1
    val bar2 = 1 <warning descr="SSR">//</warning>
    <warning descr="SSR">/**/</warning>
    val bar3 = 1
    val bar4 = 1 <warning descr="SSR">/**/</warning>
    val bar5 <warning descr="SSR">/**/</warning> = 1
    <warning descr="SSR">/**
     *
     */</warning>
    val bar6 = 1
    val bar7 = 1

    print(bar1 + bar2 + bar3 + bar4 + bar5 + bar6 + bar7)
}