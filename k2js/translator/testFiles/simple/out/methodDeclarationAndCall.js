var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , method:function(){
    {
      return true;
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
    return test.method();
  }
}
}, {Test:classes.Test});
foo.initialize();
