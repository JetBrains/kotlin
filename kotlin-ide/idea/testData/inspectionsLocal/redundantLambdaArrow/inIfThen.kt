// PROBLEM: none
fun test(): (Int) -> Int {
    return if (true) { <caret>_ -> 42 } else { _ -> 42 }
}