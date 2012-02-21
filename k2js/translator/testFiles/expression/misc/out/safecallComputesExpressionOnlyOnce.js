var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $i = 0;
}
, get_i:function(){
  return $i;
}
, set_i:function(tmp$0){
  $i = tmp$0;
}
, test:function(){
  var tmp$1;
  var tmp$0;
  {
    return tmp$0 = foo.get_i() , (tmp$1 = tmp$0 , (foo.set_i(tmp$0 + 1) , tmp$1));
  }
}
, box:function(){
  {
    if (foo.get_i() != 0)
      return false;
    foo.test() + 1;
    if (foo.get_i() != 1)
      return false;
    foo.test() - 2;
    if (foo.get_i() != 2)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
