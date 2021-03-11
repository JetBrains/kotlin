import kotlin.collections.listOf as list

fun foo() {
    lis<caret>
}

// EXIST: { lookupString: "list", itemText: "list", tailText: "() (kotlin.collections.listOf)" }
