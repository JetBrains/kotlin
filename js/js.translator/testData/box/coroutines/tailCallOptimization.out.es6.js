function *delay(timeMillis, $completion) {
  if (timeMillis <= 0n)
    return Unit_getInstance();
  // Inline function 'suspendCancellableCoroutine' call
  // Inline function 'kotlin.js.suspendCoroutineUninterceptedOrReturnJS' call
  var dummy = WrapperContinuation.new_kotlin_js_WrapperContinuation_6pcgol_k$($completion);
  yield* suspendOrReturn(Unit_getInstance(), dummy, $completion);
  return Unit_getInstance();
}
