function *delay(timeMillis, $completion) {
  if (timeMillis <= 0n)
    return Unit$instance;
  // Inline function 'suspendCancellableCoroutine' call
  // Inline function 'kotlin.js.suspendCoroutineUninterceptedOrReturnJS' call
  (yield () => {
    $completion;
    return Unit$instance;
  });
  return Unit$instance;
}
