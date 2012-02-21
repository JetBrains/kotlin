var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
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
  return {Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.Test).get_a() == 3;
  }
}
}, {Test:classes.Test});
foo.initialize();
