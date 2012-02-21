var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , foo:function(){
    {
      return 610;
    }
  }
  });
  return {SimpleClass:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new Anonymous.SimpleClass;
    if (c.foo() == 610) {
      return 'OK';
    }
    return 'FAIL';
  }
}
}, {SimpleClass:classes.SimpleClass});
Anonymous.initialize();
