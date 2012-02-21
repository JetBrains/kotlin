var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, get_abs:function(receiver){
  var tmp$0;
  if (receiver > 0)
    tmp$0 = receiver;
  else 
    tmp$0 = -receiver;
  {
    return tmp$0;
  }
}
, box:function(){
  {
    if (foo.get_abs(4) != 4)
      return false;
    if (foo.get_abs(-5.2) != 5.2)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
