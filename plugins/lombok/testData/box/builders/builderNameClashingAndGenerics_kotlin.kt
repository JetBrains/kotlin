import lombok.Builder

@Builder(builderClassName = "NameClashingBuilder")
class NameClashing<T>(val field: T) {
    class NameClashingBuilder<T> {
        fun customMethod(t: T) {
            field(t)
        }
    }

    companion object {
        fun test() {
            val builder: NameClashingBuilder<String> = builder<String>()
            builder.field("FAIL").customMethod("OK")
            val build: NameClashing<String> = builder.build()
            val result: String = build.field
        }
    }
}

fun testNameClashing() {
    val builder: NameClashing.NameClashingBuilder<String> = NameClashing.builder<String>()
    builder.field("FAIL").customMethod("OK")
    val obj: NameClashing<String> = builder.build()
    assertEquals("OK", obj.field)
}

fun box(): String {
    NameClashing.test()
    testNameClashing()
    return "OK"
}
