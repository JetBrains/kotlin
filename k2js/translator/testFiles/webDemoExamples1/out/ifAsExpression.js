var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    Kotlin.println(Anonymous.max(Kotlin.parseInt(args[0]), Kotlin.parseInt(args[1])));
  }
}
, max:function(a, b){
  var tmp$0;
  if (a > b)
    tmp$0 = a;
  else 
    tmp$0 = b;
  {
    return tmp$0;
  }
}
}, {});
Anonymous.initialize();
