package a

class KotlinSpringBean(var fooNew: Int)

fun test() {
    val bean = KotlinSpringBean(1)
    bean.fooNew
    bean.fooNew = 2
}