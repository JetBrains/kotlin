import org.jetbrains.annotations.PropertyKey

fun message(@PropertyKey(resourceBundle = "PropertyKeysNoPrefix") key: String) = key

fun test() {
    message("<caret>foo")
}

// EXIST: { lookupString: "foo.bar", itemText: "foo.bar", tailText: "1", typeText: "PropertyKeysNoPrefix" }
// EXIST: { lookupString: "bar.baz", itemText: "bar.baz", tailText: "2", typeText: "PropertyKeysNoPrefix" }
// EXIST: { lookupString: "foo.bar.baz", itemText: "foo.bar.baz", tailText: "3", typeText: "PropertyKeysNoPrefix" }
// EXIST: { lookupString: "foo.test", itemText: "foo.test", tailText: "4", typeText: "PropertyKeysNoPrefix" }
// NOTHING_ELSE