@Deprecate<caret>
fun foo() { }

// INVOCATION_COUNT: 2
// WITH_ORDER
// EXIST: { itemText: "Deprecated", tailText: " (kotlin)" }
// EXIST_JAVA_ONLY: { itemText: "Deprecated", tailText: " (java.lang)" }
// NOTHING_ELSE
