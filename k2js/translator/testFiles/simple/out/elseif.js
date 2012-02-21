var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, bor:function(){
  {
    var a = 2;
    var b = 3;
    var c = 4;
    if (a < 2) {
      return a;
    }
     else if (a > 2) {
      return b;
    }
     else if (a == c) {
      return c;
    }
     else {
      return 5;
    }
  }
}
, box:function(){
  {
    return foo.bor() == 5;
  }
}
}, {});
foo.initialize();
