fun foo(s: String, list: List<String>?) {
    if (s in <caret>)
}

// ABSENT: { lookupString:"list", itemText: "list" }
// EXIST: { lookupString:"list", itemText: "!! list", typeText:"List<String>?" }
// EXIST: { lookupString:"list", itemText: "?: list", typeText:"List<String>?" }
