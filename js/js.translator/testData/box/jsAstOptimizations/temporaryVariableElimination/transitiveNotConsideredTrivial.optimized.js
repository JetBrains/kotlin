function se() {
  var _unary__edvuaz = global;
  global = _unary__edvuaz + 1 | 0;
  return _unary__edvuaz;
}
function box() {
  var a = se();
  var b = a;
  var result = b + b | 0;
  if (!(result === 2))
    return 'fail: ' + result;
  return 'OK';
}
