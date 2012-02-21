var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, sum:function(param1, param2){
  {
    return param1 + param2;
  }
}
, box:function(){
  {
    return foo.sum(1, 5) == 6;
  }
}
}, {});
foo.initialize();
