import kotlin.collections.firstOrNull as aaa

fun foo() {
    listOf(1, 2).aa<caret>
}

// EXIST: { lookupString: "aaa", itemText: "aaa", tailText: "() for List<T> (kotlin.collections.firstOrNull)" }
