fun foo(): Any? = TODO<caret>

// WITH_ORDER
// EXIST: { itemText: "TODO", tailText:"() (kotlin)" }
// EXIST: { itemText: "TODO", tailText:" (other)" }
