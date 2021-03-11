import java.io.File

class C : File("") {
    override fun isFile(): Boolean {
        return true
    }
}

fun f(pair: Pair<out File, out Any>) {
    if (pair.first !is C) return
    pair.first.<caret>
}

// EXIST: {"lookupString":"absolutePath","tailText":" (from getAbsolutePath())","typeText":"String","attributes":"bold","allLookupStrings":"absolutePath, getAbsolutePath","itemText":"absolutePath"}
// EXIST: { lookupString: "isFile", itemText: "isFile", tailText: " (from isFile())", typeText: "Boolean", attributes: "bold" }
