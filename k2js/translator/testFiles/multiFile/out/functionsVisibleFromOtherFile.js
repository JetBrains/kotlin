var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(){
  {
    return 3;
  }
}
, box:function(){
  {
    return foo.f() == 3;
  }
}
}, {});
foo.initialize();
