function *delay(timeMillis, $completion) {
  if (timeMillis <= 0n)
    return Unit_getInstance();
  // Inline function 'suspendCancellableCoroutine' call
  (yield () => Unit_getInstance());
  return Unit_getInstance();
}
