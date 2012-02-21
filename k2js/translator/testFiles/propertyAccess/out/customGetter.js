var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , get_a:function(){
    {
      return 5;
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
    return test.get_a() == 5;
  }
}
}, {Test:classes.Test});
foo.initialize();
