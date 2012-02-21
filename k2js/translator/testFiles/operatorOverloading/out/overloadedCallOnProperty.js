var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$b = 0;
  }
  , get_b:function(){
    return this.$b;
  }
  , set_b:function(tmp$0){
    this.$b = tmp$0;
  }
  , inc:function(){
    {
      var res = new foo.MyInt;
      res.set_b(this.get_b());
      res.set_b(res.get_b() + 1);
      return res;
    }
  }
  });
  return {MyInt:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $a = new foo.MyInt;
}
, get_a:function(){
  return $a;
}
, set_a:function(tmp$0){
  $a = tmp$0;
}
, box:function(){
  {
    foo.set_a(foo.get_a().inc());
    foo.set_a(foo.get_a().inc());
    return foo.get_a().get_b() == 2;
  }
}
}, {MyInt:classes.MyInt});
foo.initialize();
