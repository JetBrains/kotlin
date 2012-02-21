var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $b = 3;
}
, get_b:function(){
  return $b;
}
, box:function(){
  {
    return foo.get_b() == 3;
  }
}
}, {});
foo.initialize();
