// WITH_STDLIB
// ISSUE: KT-87375

annotation class AllOpen

@AllOpen
class Foo {
    val ebf: List<String> <!NON_FINAL_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>field = mutableListOf()<!>
}
