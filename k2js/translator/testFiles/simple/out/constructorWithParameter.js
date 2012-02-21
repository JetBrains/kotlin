var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a){
    this.$b = a;
  }
  , get_b:function(){
    return this.$b;
  }
  });
  return {Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var test = new foo.Test(1);
    return test.get_b() == 1;
  }
}
}, {Test:classes.Test});
foo.initialize();
