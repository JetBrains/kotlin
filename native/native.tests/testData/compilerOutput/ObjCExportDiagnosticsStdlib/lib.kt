package pkg.stdlib

// Exposing stdlib Appendable and StringBuilder in the public API forces ObjCExport
// to emit them in the framework header. Their overloaded methods (append, insert,
// appendRange, insertRange) and StringBuilder(content: ...) constructors collide on
// ObjC selectors and must not fail under -Xbinary=objcExportErrorOnNameCollisions=true.
fun useAppendable(a: Appendable): Appendable = a

fun useStringBuilder(sb: StringBuilder): StringBuilder = sb
