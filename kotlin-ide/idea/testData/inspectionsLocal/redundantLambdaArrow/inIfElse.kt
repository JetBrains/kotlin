// PROBLEM: none
fun test(): (Int) -> Int {
    return if (true) { _ -> 42 } else { <caret>_ -> 42 }
}