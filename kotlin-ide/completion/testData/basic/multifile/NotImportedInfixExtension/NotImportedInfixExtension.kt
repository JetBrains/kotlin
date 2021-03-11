package pack

class C

fun f() {
    C() <caret>
}

// ABSENT: "xxx"
// EXIST: { lookupString: "yyy", attributes: "bold" }
// ABSENT: "zzz"
// ABSENT: "extensionProp"
