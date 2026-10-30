function id(x) {
  log = log + ('' + x + ';');
  return x;
}
function box() {
  var a = id(2);
  var b = id(3);
  if (id(4) > id(5) || a > b)
    return 'fail condition';
  if (!(log === '2;3;4;5;'))
    return 'fail log: ' + log;
  return 'OK';
}
