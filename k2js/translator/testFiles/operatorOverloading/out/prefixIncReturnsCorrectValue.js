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
  , dec:function(){
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
}
, box:function(){
  {
    var c = new foo.MyInt;
    var d = c = c.dec();
    return c.get_b() == 1;
  }
}
}, {MyInt:classes.MyInt});
foo.initialize();
