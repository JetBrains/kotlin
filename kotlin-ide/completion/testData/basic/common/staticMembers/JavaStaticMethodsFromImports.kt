import java.util.Collections.singletonList

fun foo() {
    singl<caret>
}

// INVOCATION_COUNT: 1
// EXIST_JAVA_ONLY: {"lookupString":"singleton","tailText":"(o: T!) (java.util)","typeText":"MutableSet<T!>","attributes":"","allLookupStrings":"singleton","itemText":"Collections.singleton"}
// ABSENT: { itemText: "Collections.singletonList" }
// EXIST_JAVA_ONLY: { itemText: "singletonList", tailText: "(o: T!)", typeText: "(Mutable)List<T!>!", attributes: "" }
