function test1() {
  global = global + 1 | 0;
  return global;
}
function test2() {
  global = global + 1 | 0;
  return global;
}
function test3() {
  global = global + 1 | 0;
  var _unary__edvuaz = global;
  global = _unary__edvuaz - 1 | 0;
  var b = _unary__edvuaz;
  return b + b | 0;
}
function box() {
  var result = test1();
  if (!(result === 1))
    return 'fail1: ' + result;
  result = test2();
  if (!(result === 2))
    return 'fail2: ' + result;
  result = test3();
  if (!(result === 6))
    return 'fail3: ' + result;
  return 'OK';
}

