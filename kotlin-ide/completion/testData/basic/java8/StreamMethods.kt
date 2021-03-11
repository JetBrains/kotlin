fun foo(x: List<String>) {
    x.stream().<caret>
}

// EXIST: {"lookupString":"allMatch","tailText":" {...} (predicate: ((String!) -> Boolean)!)","typeText":"Boolean","attributes":"bold"}
// EXIST: {"lookupString":"allMatch","tailText":"(predicate: Predicate<in String!>!)","typeText":"Boolean","attributes":"bold"}
// EXIST: {"lookupString":"count","tailText":"()","typeText":"Long","attributes":"bold"}
