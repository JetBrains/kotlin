fun foo() {
    val cast1: Int = 1 as Int
    val cast2: Int = null as Int
    val cast3: Float = (1 as Int).toFloat()
    val nya: Int = null
    val cast4: Int = nya as Int
}