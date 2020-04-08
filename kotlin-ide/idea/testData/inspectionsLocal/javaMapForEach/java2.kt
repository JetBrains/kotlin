// RUNTIME_WITH_FULL_JDK
fun test(hashMap: HashMap<Int, String>) {
    hashMap.<caret>forEach { key, value ->
        foo(key, value)
    }
}

fun foo(i: Int, s: String) {}