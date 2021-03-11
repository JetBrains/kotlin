fun foo(vararg strings: String, options: Int = 0){ }

fun bar(s: String){
    foo("", <caret>)
}

// EXIST: s
