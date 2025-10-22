inline fun inlineFunction(x: Int) {}

var OK : Any? = null
val flag1 = 1
val flag2 = 2

fun check() = true

fun sep(comment: String) {}

fun setOK(): Int {
    OK = "OK"
    return 1
}

// EXPECT_GENERATED_JS: function=box expect=inlineEmptyFunctionTest.js
fun box(): String {
    sep("Simple call")
    inlineFunction(1)
    inlineFunction(setOK())

    sep("Call in if")
    if (flag1 != 0) {
        if (OK == "OK" && flag1 == 1 && flag2 is Int && check()) {
            inlineFunction(2)
        }
    }

    sep("Call in else")
    if (flag1 != 0) {
        if (OK == "OK" && flag1 == 1 && flag2 is Int && check()) {
            check() // non inline call
            check() // non inline call
        } else {
            inlineFunction(3)
        }
    }

    sep("Call in while")
    while (OK != "OK") {
        inlineFunction(4)
    }

    sep("Call in when")
    when (OK) {
        is String -> inlineFunction(5)
        is Number -> inlineFunction(6)
        else -> inlineFunction(7)
    }

    sep("Call in try/catch/finally")
    try {
        inlineFunction(8)
    } catch (e: Exception) {
        inlineFunction(9)
    } finally {
        inlineFunction(10)
    }

    sep("End")
    return OK as String
}
