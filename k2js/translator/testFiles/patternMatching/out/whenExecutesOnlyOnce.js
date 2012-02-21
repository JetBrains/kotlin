var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$1;
    var tmp$0;
    var a = 0;
    for (tmp$0 = 0; tmp$0 < 3; ++tmp$0) {
      if (tmp$0 == 0)
        if (Kotlin.isType(new foo.A, foo.A)) {
          tmp$1 = a++;
          break;
        }
      if (tmp$0 == 1)
        if (Kotlin.isType(new foo.A, foo.A)) {
          tmp$1 = a++;
          break;
        }
      if (tmp$0 == 2)
        tmp$1 = a++;
    }
    tmp$1;
    return a == 1;
  }
}
}, {A:classes.A});
foo.initialize();
