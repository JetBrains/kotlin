function test$lambda($x) {
  return () => {
    const tmp0_elvis_lhs = $x;
    let tmp;
    if (tmp0_elvis_lhs == null) {
      // Inline function 'kotlin.run' call
      return 'OK';
    } else {
      tmp = tmp0_elvis_lhs;
    }
    const z = tmp;
    return 'Fail 1: ' + z;
  };
}
