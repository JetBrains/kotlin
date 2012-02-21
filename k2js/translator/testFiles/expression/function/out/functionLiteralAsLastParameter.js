var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(a){
  {
    return a(1);
  }
}
, box:function(){
  {
    if (foo.f(function(it){
      {
        return it + 2;
      }
    }
    ) != 3)
      return false;
    if (foo.f(function(a){
      {
        return a * 300;
      }
    }
    ) != 300)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
