// ISSUE: KT-57376
package kt57376

actual interface A: (Throwable?) -> Unit {
    actual override fun invoke(cause: Throwable?)
}
