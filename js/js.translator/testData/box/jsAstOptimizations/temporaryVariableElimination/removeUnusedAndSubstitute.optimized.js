function test(a) {
  var tmp1;
  var tmp = log;
  var tmp_0;
  if (a === 3) {
    tmp1 = 1;
    tmp_0 = tmp1;
  } else {
    tmp1 = -100;
    tmp_0 = tmp1;
  }
  log = tmp + tmp_0 | 0;
  return a;
}
function box() {
  if (!(test(3) === 3))
    return 'fail1';
  if (!(log === 1))
    return 'fail2';
  return 'OK';
}

