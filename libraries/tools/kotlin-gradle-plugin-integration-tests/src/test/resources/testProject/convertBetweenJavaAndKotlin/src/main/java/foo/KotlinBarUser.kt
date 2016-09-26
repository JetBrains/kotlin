package foo

class KotlinBarUser {
    fun use(bar: Bar) {
        println("Used from kotlin Bar.getX() = ${bar.x}")
    }
}