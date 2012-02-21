var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$gc = 0;
    this.$sc = 0;
    this.$b = new foo.MyInt(0);
  }
  , get_gc:function(){
    return this.$gc;
  }
  , set_gc:function(tmp$0){
    this.$gc = tmp$0;
  }
  , get_sc:function(){
    return this.$sc;
  }
  , set_sc:function(tmp$0){
    this.$sc = tmp$0;
  }
  , get_b:function(){
    {
      this.set_gc(this.get_gc() + 1);
      return this.$b;
    }
  }
  , set_b:function(a){
    {
      this.set_sc(this.get_sc() + 1);
    }
  }
  });
  var tmp$1 = Kotlin.Class.create({initialize:function(i){
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
  return {MyInt:tmp$1, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$1;
    var tmp$0;
    var t = new foo.A;
    var d = (tmp$0 = t.get_b() , (tmp$1 = tmp$0 , (t.set_b(tmp$0.inc()) , tmp$1)));
    return t.get_sc() == 1 && t.get_gc() == 1;
  }
}
}, {MyInt:classes.MyInt, A:classes.A});
foo.initialize();
