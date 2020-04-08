fun foo(x: Any) = when (x) {
    Comparable::class, Iterable::class, String::class, // trailing comma
    -> println(1)
    else -> println(3)
}

fun foo(x: Any) {
    when (x) {
        Comparable::class, Iterable::class, String::class /*// trailing comma*/ -> println(1)
        else -> println(3)
    }

    when (x) {
        Comparable::class, Iterable::class,
        String::class /*// trailing comma*/ -> println(1)
        else -> println(3)
    }

    when (x) {
        Comparable::class, Iterable::class,
        String::class, /*// trailing comma*/
        -> println(1)
        else -> println(3)
    }

    when (x) {
        Comparable::class, Iterable::class, String::class /*// trailing comma*/ -> println(1)
        else -> println(3)
    }

    when (x) {
        1 -> {

        }
        else -> println(3)
    }

    when (x) {
        1 -> {

        }
        else -> println(3)
    }

    when (x) {
        1
        -> {

        }
        else -> println(3)
    }

    when (x) {
        1, 2,
        3 /**/ -> {

        }
        else -> println(3)
    }

    when (val c = x) {
        1, 2,
        3 /**/ -> {

        }
        else -> println(3)
    }

    when {
        x in coll
        -> {

        }
        else -> println(3)
    }
}

// SET_TRUE: ALLOW_TRAILING_COMMA