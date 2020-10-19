annotation class MyAnn

<warning descr="SSR">var tlProp: Int = 1
    @MyAnn get() { return field * 3 }</warning>

class A {
    <warning descr="SSR">var myProp: Int = 1
        @MyAnn get() = field * 3</warning>
    var myPropTwo = 2
        get() = field * 2
}