var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 1;
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
, f:function(){
  {
    var a = new foo.Test;
    var b = new foo.Test;
    b.set_a(100);
    return b.get_a() - a.get_a();
  }
}
}, {Test:classes.Test});
foo.initialize();
