// LANGUAGE: +NameBasedDestructuring
// ISSUE: KT-81555
// simple.kts
var result = "getter must be called"

class C {
    val myProp: String
        get() {
            result = "OK"
            return ""
        }
}

(val _ = myProp) = C()

// expected: result: OK