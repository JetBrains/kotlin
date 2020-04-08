import java.io.FileInputStream as FileInputStreamMy

fun foo(): FileInputS<caret>

// WITH_ORDER
// EXIST: { lookupString: "FileInputStreamMy", itemText: "FileInputStreamMy", tailText: " (java.io.FileInputStream)" }
// EXIST: { lookupString: "FileInputStream", itemText: "FileInputStream", tailText: " (java.io)" }
