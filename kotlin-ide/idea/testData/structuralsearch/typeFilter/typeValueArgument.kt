fun print(x: Any) { x.hashCode() }

fun main() {
    <warning descr="SSR">print("1")</warning>
    print(1)
}