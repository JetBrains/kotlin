// SNIPPET

var reads = 0
var writes = 0
var observable: Int = 1
    get() {
        reads++
        return field
    }
    set(value) {
        writes++
        field = value
    }

val first = observable
observable = 2
val second = observable

// EXPECTED: first == 1
// EXPECTED: second == 2
// EXPECTED: reads == 2
// EXPECTED: writes == 1
