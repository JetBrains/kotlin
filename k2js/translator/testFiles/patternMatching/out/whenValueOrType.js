var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(){
  }
  });
  return {A:tmp$1, B:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$3;
    var tmp$2;
    var tmp$1;
    var tmp$0;
    var c = 0;
    var a = new foo.A;
    var b = null;
    for (tmp$0 = 0; tmp$0 < 4; ++tmp$0) {
      if (tmp$0 == 0)
        if (a == null) {
          tmp$1 = c = 10;
          break;
        }
      if (tmp$0 == 1)
        if (Kotlin.isType(a, foo.B)) {
          tmp$1 = c = 10000;
          break;
        }
      if (tmp$0 == 2)
        if (Kotlin.isType(a, foo.A)) {
          tmp$1 = c = 20;
          break;
        }
      if (tmp$0 == 3)
        tmp$1 = c = 1000;
    }
    tmp$1;
    for (tmp$2 = 0; tmp$2 < 3; ++tmp$2) {
      if (tmp$2 == 0)
        if (b == null) {
          tmp$3 = c += 5;
          break;
        }
      if (tmp$2 == 1)
        if (Kotlin.isType(b, foo.B)) {
          tmp$3 = c += 100;
          break;
        }
      if (tmp$2 == 2)
        tmp$3 = c = 1000;
    }
    tmp$3;
    return c == 25;
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
