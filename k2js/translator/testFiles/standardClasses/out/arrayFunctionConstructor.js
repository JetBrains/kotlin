var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $f = function(i){
    {
      return i + 1;
    }
  }
  ;
  $a = Kotlin.arrayFromFun(3, foo.get_f());
}
, get_f:function(){
  return $f;
}
, get_a:function(){
  return $a;
}
, box:function(){
  {
    return foo.get_a()[0] == 1 && foo.get_a()[2] == 3 && foo.get_a()[1] == 2;
  }
}
}, {});
foo.initialize();
