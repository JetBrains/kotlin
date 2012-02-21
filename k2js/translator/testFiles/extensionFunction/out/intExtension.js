var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, quadruple:function(receiver){
  {
    return receiver * 4;
  }
}
, box:function(){
  {
    return foo.quadruple(3) == 12;
  }
}
}, {});
foo.initialize();
