var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a = 3;
}
, get_a:function(){
  return $a;
}
, set_a:function(tmp$0){
  $a = tmp$0;
}
, box:function(){
  {
    foo.set_a(foo.get_a() - 10);
    return foo.get_a() == -7;
  }
}
}, {});
foo.initialize();
