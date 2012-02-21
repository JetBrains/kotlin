var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 100;
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
    var test = new foo.Test;
    test.set_a(1);
    return 1 == test.get_a();
  }
}
}, {Test:classes.Test});
foo.initialize();
