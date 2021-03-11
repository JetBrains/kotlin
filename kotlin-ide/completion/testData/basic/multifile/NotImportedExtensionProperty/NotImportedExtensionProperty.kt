package first

fun firstFun() {
    val a = ""
    a.hello<caret>
}

// EXIST: { lookupString: "helloProp1", attributes: "bold" }
// EXIST: { lookupString: "helloProp2", attributes: "bold" }
// ABSENT: helloProp3
// ABSENT: helloProp4
// NOTHING_ELSE
