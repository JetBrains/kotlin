fun foo(p1: Int, p2: Int, p3: Int, p4: Int, p5: Int) {}

fun usage(param: Int) {
    foo(0, p2 = 10, 20, p4 = 30, <caret>)
}

// LANGUAGE_VERSION: 1.4
// EXIST: param
// EXIST: { itemText: "p5 =" }
// ABSENT: { itemText: "p1 =" }
// ABSENT: { itemText: "p2 =" }
// ABSENT: { itemText: "p3 =" }
// ABSENT: { itemText: "p4 =" }
