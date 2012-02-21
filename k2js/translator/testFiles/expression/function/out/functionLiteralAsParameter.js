var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, apply:function(f, t){
  {
    return f(t);
  }
}
, box:function(){
  {
    return foo.apply(function(a){
      {
        return a + 5;
      }
    }
    , 3) == 8;
  }
}
}, {});
foo.initialize();
