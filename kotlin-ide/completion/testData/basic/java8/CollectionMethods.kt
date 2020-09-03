fun foo(x: MutableList<String>) {
    x.<caret>
}

// EXIST: {"lookupString":"stream","tailText":"()","typeText":"Stream<String>"}
// EXIST: {"lookupString":"removeIf","tailText":"(filter: Predicate<in String>)","typeText":"Boolean"}
// EXIST: {"lookupString":"removeIf","tailText":" {...} (filter: (String) -> Boolean)","typeText":"Boolean"}
