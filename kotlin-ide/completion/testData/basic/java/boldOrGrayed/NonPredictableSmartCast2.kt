import java.io.File

class C : File("") {
    val v: Int = 0
}

fun f(pair: Pair<out Any, out Any>) {
    if (pair.first !is C) return
    pair.first.<caret>
}

// EXIST: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!", attributes: "grayed" }
