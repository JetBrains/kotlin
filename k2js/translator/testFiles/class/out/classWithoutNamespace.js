var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a){
    this.$a = a;
    {
      this.$a = 3;
    }
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
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new Anonymous.A(1)).get_a() == 3;
  }
}
}, {A:classes.A});
Anonymous.initialize();
