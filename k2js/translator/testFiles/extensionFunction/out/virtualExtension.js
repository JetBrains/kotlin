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
  , modify:function(receiver){
    {
      return receiver * 3;
    }
  }
  , eval_0:function(){
    {
      return this.modify(this.get_a());
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var a = new foo.A(4);
    return a.eval_0() == 12;
  }
}
}, {A:classes.A});
foo.initialize();
