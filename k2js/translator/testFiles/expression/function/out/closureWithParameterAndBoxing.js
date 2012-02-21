var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    if (foo.apply(5, function(arg){
      {
        return arg + 13;
      }
    }
    ) == 18)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
, apply:function(arg, f){
  {
    return f(arg);
  }
}
}, {});
foo.initialize();
