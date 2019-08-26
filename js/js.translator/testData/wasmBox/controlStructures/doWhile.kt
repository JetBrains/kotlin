fun box(): String {
    var x = 0
    do x++ while (x < 5)
    if (x != 5) return "Fail 1"
    
    var y = 0
    do { y++ } while (y < 5)
    if (y != 5) return "Fail 2"
    
    var z = "z"
    do { z += z } while (z.length < 5)
    if (z != "zzzzzzzz") return z
    
    return "OK"
}
