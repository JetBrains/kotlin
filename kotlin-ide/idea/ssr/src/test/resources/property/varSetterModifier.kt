class A {
    <warning descr="SSR">var myProp: Int = 1
        private set(value) {
            field = value * 3
        }</warning>

    var myPropTwo = 2
        set(value) {
            field = value * 3
        }
}