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
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(a){
    this.super_init(a);
  }
  , modify:function(receiver){
    {
      return receiver - 2;
    }
  }
  });
  return {B:tmp$1, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A(4)).eval_0() == 12 && (new foo.A(2)).eval_0() == 6 && (new foo.B(3)).eval_0() == 1;
  }
}
}, {A:classes.A, B:classes.B});
foo.initialize();
