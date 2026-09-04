function test1(n) {
  var tmp;
  if (n > 0) {
    tmp = 23;
  }
  return tmp;
}
function box() {
  var result = test1(5);
  if (!(result === 23))
    return 'fail1: ' + result;
  result = test1(-5);
  if (result === 23)
    return 'fail2: ' + result;
  return 'OK';
}
