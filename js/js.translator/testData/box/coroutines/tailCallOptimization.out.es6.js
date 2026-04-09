function *delay(timeMillis, $completion) {
  if (timeMillis <= fromInt_0(0))
    return Unit_getInstance();
  // Inline function 'suspendCancellableCoroutine' call
  // Inline function 'kotlin.js.suspendCoroutineUninterceptedOrReturnJS' call
  (yield () => Unit_getInstance());
  return Unit_getInstance();
}
