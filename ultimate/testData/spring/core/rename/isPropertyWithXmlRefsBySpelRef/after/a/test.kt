package a

class KotlinSpringBean(value: Int) {
    var isFooNew: Boolean = false
}

fun test() {
    val bean = KotlinSpringBean(1)
    bean.isFooNew
    bean.isFooNew = true
}