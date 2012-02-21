var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$p = true;
  }
  , get_p:function(){
    return this.$p;
  }
  });
  return {Test:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.Test).get_p();
  }
}
}, {Test:classes.Test});
foo.initialize();
