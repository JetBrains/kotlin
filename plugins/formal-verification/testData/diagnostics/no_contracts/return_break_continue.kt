fun <!VIPER_TEXT!>test_return<!>(): Int {
    return 0
    return 1
}

fun <!VIPER_TEXT!>return_from_loop<!>(): Int {
    while (true) {
        return 0
    }
    return 1
}

fun <!VIPER_TEXT!>while_break<!>(b: Boolean): Int {
    var i = 0
    while (b) {
        i = 1
        break
    }
    return i
}

fun <!VIPER_TEXT!>while_continue<!>() {
    var b = true
    while (b) {
        b = false
        continue
    }
}

fun <!VIPER_TEXT!>while_nested<!>(b: Boolean) {
    while(b){
        while (b){
            break
        }
        continue
        while (b){
            continue
        }
        break
    }
}

fun <!VIPER_TEXT!>labelled_break<!>(b: Boolean) {
    loop1@ while(b){
        loop2@ while (b){
            break@loop1
            break@loop2
            break
        }
        break@loop1
        break
    }
}

fun <!VIPER_TEXT!>labelled_continue<!>(b: Boolean) {
    loop1@ while(b){
        loop2@ while (b){
            continue@loop1
            continue@loop2
            continue
        }
        continue@loop1
        continue
    }
}

fun <!VIPER_TEXT!>labelled_while_shadowing<!>(b: Boolean) {
    loop1@ while(b){
        loop1@ while (b){
            break@loop1
            continue@loop1
        }
        loop1@ while (b){
            break@loop1
            continue@loop1
        }
        break@loop1
        continue@loop1
    }
}