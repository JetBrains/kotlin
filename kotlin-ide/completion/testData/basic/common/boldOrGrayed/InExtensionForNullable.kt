fun String.forString(){}

fun globalFun(){}

fun String?.foo() {
    <caret>
}

// EXIST: { lookupString: "globalFun", attributes: "" }
// EXIST: { lookupString: "compareTo", attributes: "grayed" }
// EXIST: { lookupString: "forString", attributes: "grayed" }
