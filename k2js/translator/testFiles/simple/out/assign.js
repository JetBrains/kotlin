var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(){
  {
    var x = 1;
    x = x + 1;
    return x;
  }
}
, box:function(){
  {
    return foo.f() == 2;
  }
}
}, {});
foo.initialize();
