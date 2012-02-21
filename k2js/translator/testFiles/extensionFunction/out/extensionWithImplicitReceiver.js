var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, same:function(receiver){
  {
    return receiver;
  }
}
, quadruple:function(receiver){
  {
    return foo.same(receiver) * 4;
  }
}
, box:function(){
  {
    return foo.quadruple(3) == 12;
  }
}
}, {});
foo.initialize();
