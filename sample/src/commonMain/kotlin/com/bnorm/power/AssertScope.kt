package com.bnorm.power

import kotlin.contracts.*

typealias LazyMessage = () -> Any

interface AssertScope {
  fun assert(assertion: Boolean, lazyMessage: LazyMessage? = null)
}

@OptIn(ExperimentalContracts::class)
fun assert(assertion: Boolean, lazyMessage: LazyMessage? = null) {
  contract { returns() implies assertion }
  if (!assertion) {
    throw AssertionError(lazyMessage?.invoke()?.toString())
  }
}

private class SoftAssertScope : AssertScope {
  private val assertions = mutableListOf<Throwable>()

  override fun assert(assertion: Boolean, lazyMessage: LazyMessage?) {
    if (!assertion) {
      assertions.add(AssertionError(lazyMessage?.invoke()?.toString()))
    }
  }

  fun close(exception: Throwable? = null) {
    if (assertions.isNotEmpty()) {
      val base = exception ?: AssertionError("Multiple failed assertions")
      for (assertion in assertions) {
        base.addSuppressed(assertion)
      }
      throw base
    }
  }
}

@OptIn(ExperimentalContracts::class)
fun <R> assertSoftly(block: AssertScope.() -> R): R {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  val scope = SoftAssertScope()
  val result = runCatching { scope.block() }
  scope.close(result.exceptionOrNull())
  return result.getOrThrow()
}
