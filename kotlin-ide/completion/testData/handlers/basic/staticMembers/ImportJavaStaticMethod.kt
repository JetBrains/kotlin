import java.io.File.listRoots

fun foo() {
    listR<caret>
}

// INVOCATION_COUNT: 1
// ELEMENT_TEXT: "File.listRoots"