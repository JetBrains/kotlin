// !CHECK_HIGHLIGHTING
package p

actual typealias Presence = P
sealed class P {
    object Online : P()
    object Offline : P()
}