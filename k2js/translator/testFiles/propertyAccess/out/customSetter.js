var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$a = 5;
  }
  , get_a:function(){
    return this.$a;
  }
  , set_a:function(b){
    {
      this.$a = 3;
    }
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
    test.set_a(5);
    return test.get_a() == 3;
  }
}
}, {Test:classes.Test});
foo.initialize();
