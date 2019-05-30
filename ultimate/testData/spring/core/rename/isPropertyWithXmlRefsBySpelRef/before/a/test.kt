package a

class KotlinSpringBean(value: Int) {
    var isFoo: Boolean = false
}

fun test() {
    val bean = KotlinSpringBean(1)
    bean.isFoo
    bean.isFoo = true
}