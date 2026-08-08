function *delay(timeMillis, $completion) {
  if (timeMillis <= 0n)
    return Unit$getInstance();
  // Inline function 'suspendCancellableCoroutine' call
  // Inline function 'kotlin.js.suspendCoroutineUninterceptedOrReturnJS' call
  (yield () => Unit$getInstance());
  return Unit$getInstance();
}
