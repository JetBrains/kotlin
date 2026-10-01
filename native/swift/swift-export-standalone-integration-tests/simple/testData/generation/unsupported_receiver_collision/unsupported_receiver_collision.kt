// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: packageRoot=koin.like
// FILE: koin_like.kt
@file:Suppress("EXPOSED_RECEIVER_TYPE")

package koin.like

// Shape produced by the Koin compiler plugin for every `@Module internal class X`:
// a public top-level `fun X.module(): Module` whose receiver is not exportable.
// Each receiver is erased to `Swift.Never` in an `@available(*, unavailable)` stub, so the stubs end
// up with identical Swift signatures and `swiftc` rejects them as `invalid redeclaration of 'module'`.

class Module

internal class AppModule
internal class CommonModule
internal class NativeModule

fun AppModule.module(): Module = Module()
fun CommonModule.module(): Module = Module()
fun NativeModule.module(): Module = Module()

// FILE: hidden.kt
package hidden

// The same collision without exposing internal types: receivers that are public but unsupported.

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
interface HiddenA

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
interface HiddenB

@Suppress("DEPRECATION_ERROR")
fun HiddenA.describe(): String = "A"

@Suppress("DEPRECATION_ERROR")
fun HiddenB.describe(): String = "B"
