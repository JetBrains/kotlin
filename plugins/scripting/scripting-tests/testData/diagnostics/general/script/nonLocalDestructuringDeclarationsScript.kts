// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58563

data class P(val first: Int, val second: String)

val (a, b) = P(1, "")

class C {
    val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(c, d)<!> = P(1, "")

    companion object {
        val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>[e, f]<!> = P(1, "")
    }
}

object O {
    val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(g, h)<!> = P(1, "")
}

fun local() {
    val (i, j) = P(1, "")

    class Local {
        val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(k, l)<!> = P(1, "")
    }
}

a
b

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, destructuringDeclaration, functionDeclaration, init,
integerLiteral, localClass, localProperty, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
