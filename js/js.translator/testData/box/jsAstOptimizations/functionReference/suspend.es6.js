function u_susp$ref() {
  return ($completion) => orPromise($completion, function *($completion) {
    return yield* /*#__NOINLINE__*/u_susp($completion);
  });
}
function k_susp$ref() {
  return constructCallableReference(($completion) => orPromise($completion, function *($completion) {
    return yield* /*#__NOINLINE__*/k_susp($completion);
  }), 0, 1, 5, 'k_susp');
}
