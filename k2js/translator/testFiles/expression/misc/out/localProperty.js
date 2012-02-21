var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $y = 3;
}
, get_y:function(){
  return $y;
}
, f:function(a){
  {
    var x = 42;
    var y = 50;
    return y;
  }
}
, box:function(){
  {
    return foo.f(foo.get_y());
  }
}
}, {});
foo.initialize();
