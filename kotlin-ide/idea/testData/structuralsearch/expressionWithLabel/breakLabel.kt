fun main() {
    loop@ for (i in 1..100) {
        for (j in 1..100) {
            <warning descr="SSR">break@loop</warning>
        }
    }
}