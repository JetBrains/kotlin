fun test(n: Int): String {
    return <caret>if (n !is Int) "???"
    else if (n in 0..10) "small"
    else if (n in 10..100) "average"
    else if (n in 100..1000) "big"
    else if (n == 1000000) "million"
    else if (n !in -100..-10) "good"
    else if (n is Int) "unknown"
    else "unknown"
}