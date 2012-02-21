var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a = Kotlin.nullArray(3);
}
, get_a:function(){
  return $a;
}
, box:function(){
  {
    return foo.get_a()[0] == null && foo.get_a()[1] == null && foo.get_a()[2] == null;
  }
}
}, {});
foo.initialize();
