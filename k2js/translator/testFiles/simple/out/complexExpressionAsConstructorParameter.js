var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(a, b){
    this.$c = a;
    this.$d = b;
  }
  , get_c:function(){
    return this.$c;
  }
  , get_d:function(){
    return this.$d;
  }
  });
  return {Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var test = new foo.Test(1 + 6 * 3, 10 % 2);
    return test.get_c() == 19 && test.get_d() == 0;
  }
}
}, {Test:classes.Test});
foo.initialize();
