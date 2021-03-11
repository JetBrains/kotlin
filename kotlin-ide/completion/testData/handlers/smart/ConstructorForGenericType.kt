import java.util.List

fun foo(p: HashMap<String, List<Int>>){}

fun f(){
    foo(<caret>)
}

// ELEMENT: HashMap
// TAIL_TEXT: (...) (kotlin.collections)
