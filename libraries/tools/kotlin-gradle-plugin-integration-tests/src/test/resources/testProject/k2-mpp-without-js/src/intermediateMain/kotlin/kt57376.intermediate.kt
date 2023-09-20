// ISSUE: KT-57376
package kt57376

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual interface A : (Throwable?) -> Unit {
    actual override fun invoke(cause: Throwable?)
}
