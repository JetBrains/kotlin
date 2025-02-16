
// SNIPPET

var _x = ""

var x: String
    get() = _x;
    set(value) { _x = value }

// SNIPPET

x = "OK"

// SNIPPET

x

// EXPECTED: <res> == "OK"
