var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , eval_0:function(){
    {
      return 3;
    }
  }
  , eval$0:function(a){
    {
      return 4;
    }
  }
  , eval$1:function(a){
    {
      return 5;
    }
  }
  , eval$2:function(a, b){
    {
      return 6;
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    if ((new foo.A).eval_0() != 3)
      return false;
    if ((new foo.A).eval$0(2) != 4)
      return false;
    if ((new foo.A).eval$1('3') != 5)
      return false;
    if ((new foo.A).eval$2('a', 3) != 6)
      return false;
    return true;
  }
}
}, {A:classes.A});
foo.initialize();
