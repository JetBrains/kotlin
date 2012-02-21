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
      this.set_b(this.get_b() + 1);
      return this;
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
    var tmp$1;
    var tmp$0;
    var d = (tmp$0 = foo.get_a() , (tmp$1 = tmp$0 , (foo.set_a(tmp$0.inc()) , tmp$1)));
    return foo.get_a().get_b() == 1 && d.get_b() == 1;
  }
}
}, {MyInt:classes.MyInt});
foo.initialize();
