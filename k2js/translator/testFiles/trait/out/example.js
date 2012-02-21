var classes = function(){
  var tmp$0 = Kotlin.Trait.create({get:function(index){
    {
      return null;
    }
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
  }
  });
  return {SmartArrayList:tmp$1, AL:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new foo.SmartArrayList;
    return null == c.get(0);
  }
}
}, {AL:classes.AL, SmartArrayList:classes.SmartArrayList});
foo.initialize();
