package a

class KotlinSpringBean(var foo: Int)

fun test() {
    val bean = KotlinSpringBean(1)
    bean.foo
    bean.foo = 2
}