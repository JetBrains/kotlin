package a

class KotlinSpringBean(value: Int) {
    var foo: Int = 0
}

fun test() {
    val bean = KotlinSpringBean(1)
    bean.foo
    bean.foo = 2
}