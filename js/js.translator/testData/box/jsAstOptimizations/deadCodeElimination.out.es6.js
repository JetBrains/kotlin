function test$lambda($x) {
  return () => {
    var elvis_lhs = $x;
    var tmp;
    if (elvis_lhs == null) {
      // Inline function 'kotlin.run' call
      return 'OK';
    } else {
      tmp = elvis_lhs;
    }
    var z = tmp;
    return 'Fail 1: ' + z;
  };
}
