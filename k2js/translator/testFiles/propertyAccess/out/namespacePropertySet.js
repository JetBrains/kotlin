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
, set_b:function(tmp$0){
  $b = tmp$0;
}
, box:function(){
  {
    foo.set_b(2);
    return foo.get_b() == 2;
  }
}
}, {});
foo.initialize();
