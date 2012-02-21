var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, bol:function(){
  {
    var a = 2;
    var b = 3;
    var c = 4;
    if (a < 2) {
      return a;
    }
    if (a > 2) {
      return b;
    }
    if (a == c) {
      return c;
    }
     else {
      return 5;
    }
  }
}
, box:function(){
  {
    return foo.bol() == 5;
  }
}
}, {});
foo.initialize();
