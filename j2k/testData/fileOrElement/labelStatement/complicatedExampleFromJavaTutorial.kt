test@ for (i in 0 until max + 1) {
    var n = substring.length()
    var j = i
    var k = 0
    while (n-- != 0) {
        if (searchMe.charAt(j++) !== substring.charAt(k++)) {
            continue@test
        }
    }
    foundIt = true
    break@test
}
println(if (foundIt) "Found it" else "Didn't find it")