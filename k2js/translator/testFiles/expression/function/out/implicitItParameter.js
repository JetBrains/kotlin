var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, test:function(f, p){
  {
    return f(p);
  }
}
, box:function(){
  {
    if (!foo.test(function(it){
      {
        return it + 1 == 2;
      }
    }
    , 1))
      return false;
    if (!foo.test(function(it){
      {
        return it > 1;
      }
    }
    , 3))
      return false;
    return foo.test(function(it){
      {
        return it < 1 == false;
      }
    }
    , 1);
  }
}
}, {});
foo.initialize();
