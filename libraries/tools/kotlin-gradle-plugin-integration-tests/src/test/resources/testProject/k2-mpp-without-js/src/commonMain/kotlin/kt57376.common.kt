// ISSUE: KT-57376
package kt57376

expect interface A {
    fun invoke(cause: Throwable?)
}
