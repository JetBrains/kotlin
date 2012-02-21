var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(i){
    this.$b = i;
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
}
, box:function(){
  {
    var t = new foo.MyInt(0);
    t = t.inc();
    return t.get_b() == 1;
  }
}
}, {MyInt:classes.MyInt});
foo.initialize();
