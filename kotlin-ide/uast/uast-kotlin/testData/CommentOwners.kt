/** file */

// file

/* file */

/** topLevelProperty */
val topLevelProperty: Int = 42

/** topLevelFun */
fun topLevelFun(): Unit = TODO()

/** TopLevelClass */
class TopLevelClass {

    /** secondaryConstructor */
    constructor(t: Int) {

    }

    /** classLevelProperty */
    val classLevelProperty: Int = 42

    /** classLevelMethod */
    fun classLevelMethod(): Unit = TODO()

    /** NestedClass */
    class NestedClass {

    }
}

fun funPlainCall(a: Int): Unit = TODO()
fun funNamedArgumentsCall(a: Int): Unit = TODO()

fun func(/* fun param before */ a: Int /* fun param after */) {
    // funPlainCall
    funPlainCall(/* call arg before */ 42 /* call arg after */)

    /* funNamedArgumentsCall */
    funNamedArgumentsCall(/* call arg before */ a = 42 /* call arg after */)

    // cycle
    while (true) {
        // break
        break
    }

    // if
    if (true) {

    }
    else {

    }

    // localValueDefinition
    val localValueDefinition = 42
}

/** enum */
enum class MyBooleanEnum {
    /** enum true value */
    TRUE,

    /** enum false value */
    FALSE
}