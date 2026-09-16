function foo() {
  throw Exception.c('foo');
}
function bar() {
  var tmp = foo();
  try {
    return 'result: ' + tmp;
  } catch ($p) {
    if ($p instanceof Exception) {
      var e = $p;
      return 'error';
    } else {
      throw $p;
    }
  }
}
function box() {
  try {
    bar();
  } catch ($p) {
    if ($p instanceof Exception) {
      var e = $p;
      if (e.message === 'foo') {
        return 'OK';
      }
      return 'Exception: ' + e.message;
    } else {
      throw $p;
    }
  }
  return 'Exception expected';
}
