package a

class KotlinSpringBean(value: Int) {
    var fooNew: Int = 0
}

fun test() {
    val bean = KotlinSpringBean(1)
    bean.fooNew
    bean.fooNew = 2
}