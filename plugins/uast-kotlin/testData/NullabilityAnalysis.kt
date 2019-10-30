fun dataFlowNotNull(b: Boolean) {
    val a = if (b) null else "hello"

    if (a != null) {
        println(/*NOT_NULL*/a)
    }
}

fun notNullDeclaration() {
    val a = "hello"
    println(/*NOT_NULL*/a)
}

fun nullableDeclaration(b: Boolean) {
    val a = if (b) null else "hello"
    println(/*NULLABLE*/ a)
}

fun platformTypes() {
    println(/*UNKNOWN*/ java.lang.StringBuilder().append("a"))
}

fun kotlinTypeNotNull(builder: kotlin.text.StringBuilder) {
    println(/*NOT_NULL*/builder)
}

fun kotlinTypeNullable(builder: kotlin.text.StringBuilder?) {
    println(/*NULLABLE*/builder)
}

fun elvis(b: Boolean) {
    val a = if (b) null else "hello"

    println(/*NOT_NULL*/ (a ?: return))
}

fun nullExpresion() {
    println(/*NULLABLE*/ null)
}

fun nullableParamWithDfa(p: Int?) {
    if (d != null) {
        println(/*NOT_NULL*/d)
    }
}

fun notNullIfExpression(d: Int?): Int = run {
    /*NOT_NULL*/ if (d != null) {
        return@run d
    } else {
        1
    }
}

fun platformWithIf(): String = run {
    val a = java.lang.StringBuilder().append("a").toString()
    /*NOT_NULL*/ if (a != null) {
        return@run /*NOT_NULL*/ a
    } else {
        "a"
    }
}

fun notNullIfWIthElvis(a: String?): String = run {
    if (a != null) {
        return@run /*NOT_NULL*/ a
    } else {
        return@run /*NOT_NULL*/ (a ?: "a")
    }
}

fun twoNotNull(a: String?, b: String?) {
    if (a != null && b != null) {
        println(/*NOT_NULL*/a)
    }
}

class SomeClass {
    val a: String? = if (kotlin.random.Random.nextBoolean()) null else "a"

    fun notNullIfWithElvisStrangeType() = java.util.stream.Stream.of(1, 2).map {
        if (a != null) {
            return@map /*NOT_NULL*/ a
        } else {
            return@map /*UNKNOWN*/ a ?: 0
        }
    }
}
