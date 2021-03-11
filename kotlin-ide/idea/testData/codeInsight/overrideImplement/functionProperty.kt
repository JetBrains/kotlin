// From KT-1648
interface A {
    val method:() -> Unit?
}

fun some() : A {
    return object : A {<caret>}
}

// TODO: need better selection and caret