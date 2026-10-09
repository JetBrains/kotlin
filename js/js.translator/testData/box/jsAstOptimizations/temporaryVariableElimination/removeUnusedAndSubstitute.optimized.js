function test(a) {
  var tmp1;
  var tmp;
  if (a === 3) {
    tmp1 = 1;
    tmp = tmp1;
  } else {
    tmp1 = -100;
    tmp = tmp1;
  }
  log = log + tmp | 0;
  return a;
}
function box() {
  if (!(test(3) === 3))
    return 'fail1';
  if (!(log === 1))
    return 'fail2';
  return 'OK';
}

