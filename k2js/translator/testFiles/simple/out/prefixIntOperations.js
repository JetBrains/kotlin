var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var a = 3;
    var b = ++a;
    --a;
    --a;
    return --a == 1 && b == 4;
  }
}
}, {});
foo.initialize();
