var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(a, b){
  {
    return a + b;
  }
}
, box:function(){
  {
    if (foo.f(1, 2) != 3)
      return false;
    if (foo.f(1, 3) != 4)
      return false;
    if (foo.f(3, 3) != 6)
      return false;
    if (foo.f(2, 3) != 5)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
