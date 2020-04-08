fun x(){
    val challenges = mutableListOf<() -> Unit>()
    for (c in challenges) {
        <caret>c()
    }
}

// TYPE: c -> <html>() &rarr; Unit</html>
// TYPE: c() -> <html>Unit</html>
