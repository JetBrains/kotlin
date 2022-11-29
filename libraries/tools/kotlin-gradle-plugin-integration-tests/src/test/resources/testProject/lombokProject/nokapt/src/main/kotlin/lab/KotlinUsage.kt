package lab

import com.google.common.collect.ImmutableList

fun main() {
    val obj = SomePojo()
    obj.name = "test"
    obj.age = 12
    val v = obj.isHuman
    obj.isHuman = !v
    println(obj)

    val stars = ClassWithBuilder.builder().withStars(ImmutableList.of(9, 19, 99)).build().stars
    println(stars)
}
