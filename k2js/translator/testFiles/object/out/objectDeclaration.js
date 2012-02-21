var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $test = Kotlin.object.create({initialize:function(){
    this.$c = 2;
    this.$b = 1;
  }
  , get_c:function(){
    return this.$c;
  }
  , set_c:function(tmp$0){
    this.$c = tmp$0;
  }
  , get_b:function(){
    return this.$b;
  }
  , set_b:function(tmp$0){
    this.$b = tmp$0;
  }
  });
}
, get_test:function(){
  return $test;
}
, box:function(){
  {
    if (foo.get_test().get_c() != 2)
      return false;
    if (foo.get_test().get_b() != 1)
      return false;
    foo.get_test().set_c(foo.get_test().get_c() + 10);
    if (foo.get_test().get_c() != 12) {
      return false;
    }
    return true;
  }
}
}, {});
foo.initialize();
