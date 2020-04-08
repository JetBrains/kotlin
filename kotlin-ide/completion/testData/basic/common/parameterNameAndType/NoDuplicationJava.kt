import java.sql.*

fun f(blob<caret>)

// EXIST_JAVA_ONLY: { itemText: "blob: Blob", tailText: " (java.sql)" }
// NUMBER_JAVA: 1
