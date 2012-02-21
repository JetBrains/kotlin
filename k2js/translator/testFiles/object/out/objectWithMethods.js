var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a = Kotlin.object.create({initialize:function(){
  }
  , c:function(){
    {
      return 3;
    }
  }
  , b:function(){
    {
      return 2;
    }
  }
  });
}
, get_a:function(){
  return $a;
}
, box:function(){
  {
    if (foo.get_a().c() != 3) {
      return false;
    }
    if (foo.get_a().b() != 2) {
      return false;
    }
    return true;
  }
}
}, {});
foo.initialize();
