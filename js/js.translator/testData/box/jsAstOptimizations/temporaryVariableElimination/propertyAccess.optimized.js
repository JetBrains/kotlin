function b() {
  log = log + 'b();';
  return 42;
}
function box() {
  var tmp = A$instance.a();
  var result = '' + b() + ';' + tmp;
  if (!(result === '42;23'))
    return 'fail1: ' + result;
  if (!(log === 'A.x;b();'))
    return 'fail2: ' + log;
  return 'OK';
}
