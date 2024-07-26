function test$lambda($x) {
  return function () {
    var tmp0_elvis_lhs = $x;
    var tmp;
    if (tmp0_elvis_lhs == null) {
      // Inline function 'kotlin.run' call
      // Inline function 'kotlin.contracts.contract' call
      return 'OK';
    } else {
      tmp = tmp0_elvis_lhs;
    }
    var z = tmp;
    return 'Fail 1: ' + z;
  };
}
