import java.io.File

class C : File("") {
    val v: Int = 0

    override fun isFile(): Boolean {
        return true
    }
}

fun foo(c: C) {
    c.<caret>
}

// EXIST: { lookupString: "absolutePath", itemText: "absolutePath", tailText: " (from getAbsolutePath())", typeText: "String!", attributes: "" }
// EXIST: { lookupString: "isFile", itemText: "isFile", tailText: " (from isFile())", typeText: "Boolean", attributes: "" }
// EXIST: { lookupString: "v", itemText: "v", attributes: "bold" }
