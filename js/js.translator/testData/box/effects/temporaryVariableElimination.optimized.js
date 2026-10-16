function check(value, expected) {
  if (!(value === expected))
    throw Exception.c('expected ' + expected + ' got ' + value);
  return value;
}
function read(expected) {
  return check(Global$instance.f_1, expected);
}
function write(expected) {
  var v = read(expected);
  Global$instance.f_1 = v + 1 | 0;
  return Global$instance.f_1;
}
function box() {
  // Inline function 'reset' call
  if (4 < 0)
    throw Exception.c('reset init must be >= 0');
  if (Global$instance.f_1 >= 0)
    throw Exception.c('called reset inside of another reset');
  Global$instance.f_1 = 4;
  var xx = imul(read(4), 4);
  var y = read(4);
  var c = write(4);
  var f = box$lambda(4, imul(y, 4), 6, xx);
  var g = new box$1(c, 2);
  if (!(f() > 4 && g.i() > 0))
    return 'not ok';
  Global$instance.f_1 = -1;
  // Inline function 'reset' call
  if (0 < 0)
    throw Exception.c('reset init must be >= 0');
  if (Global$instance.f_1 >= 0)
    throw Exception.c('called reset inside of another reset');
  Global$instance.f_1 = 0;
  var b = read(0) + 1 | 0;
  var i = 0;
  while (i <= 2) {
    var c_0 = b + 1 | 0;
    write(i);
    check(c_0, 2);
    i = i + 1 | 0;
  }
  Global$instance.f_1 = -1;
  return 'OK';
}

