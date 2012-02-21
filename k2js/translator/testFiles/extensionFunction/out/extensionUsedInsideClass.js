var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a){
    this.$a = a;
  }
  , get_a:function(){
    return this.$a;
  }
  , set_a:function(tmp$0){
    this.$a = tmp$0;
  }
  , eval_0:function(){
    {
      return foo.f(this);
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, f:function(receiver){
  {
    receiver.set_a(3);
    return 10;
  }
}
, box:function(){
  {
    var a = new foo.A(4);
    return a.eval_0() == 10 && a.get_a() == 3;
  }
}
}, {A:classes.A});
foo.initialize();
