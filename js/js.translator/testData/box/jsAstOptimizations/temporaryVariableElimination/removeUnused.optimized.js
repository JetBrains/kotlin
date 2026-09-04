function test1() {
  var _unary__edvuaz = global;
  global = _unary__edvuaz + 1 | 0;
  var tmp = _unary__edvuaz;
  return global;
}
function test2() {
  var tmp;
  var _unary__edvuaz = global;
  global = _unary__edvuaz + 1 | 0;
  tmp = _unary__edvuaz;
  return global;
}
function test3() {
  var _unary__edvuaz = global;
  global = _unary__edvuaz + 1 | 0;
  var a = _unary__edvuaz;
  var _unary__edvuaz_0 = global;
  global = _unary__edvuaz_0 - 1 | 0;
  var b = _unary__edvuaz_0;
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
