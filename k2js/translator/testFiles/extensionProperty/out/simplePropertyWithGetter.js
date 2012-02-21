var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, get_size:function(receiver){
  {
    return receiver.length;
  }
}
, get_quadruple:function(receiver){
  {
    return receiver * 4;
  }
}
, box:function(){
  {
    if (foo.get_size('1') != 1)
      return false;
    if (foo.get_size('11') != 2)
      return false;
    if (foo.get_size('121' + '123') != 6)
      return false;
    if (foo.get_quadruple(1) != 4)
      return false;
    if (foo.get_quadruple(0) != 0)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
