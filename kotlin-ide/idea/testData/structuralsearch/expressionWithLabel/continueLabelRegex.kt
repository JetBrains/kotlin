fun main() {
    foo@ for (i in 1..100) {
        for (j in 1..100) {
            <warning descr="SSR">continue@foo</warning>
        }
    }
    foo1@ for (i in 1..100) {
        for (j in 1..100) {
            <warning descr="SSR">continue@foo1</warning>
        }
    }
    bar@ for (i in 1..100) {
        for (j in 1..100) {
            continue@bar
        }
    }
}