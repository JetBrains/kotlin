var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(b, a){
    this.$b = b;
    this.$a = a;
  }
  , get_b:function(){
    return this.$b;
  }
  , set_b:function(tmp$0){
    this.$b = tmp$0;
  }
  , get_a:function(){
    return this.$a;
  }
  , set_a:function(tmp$0){
    this.$a = tmp$0;
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new foo.A(2, '2');
    return c.get_b() == 2 && c.get_a() == '2';
  }
}
}, {A:classes.A});
foo.initialize();
