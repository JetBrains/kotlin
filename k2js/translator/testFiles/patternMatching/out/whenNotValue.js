var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$1;
    var tmp$0;
    var a = 4;
    for (tmp$0 = 0; tmp$0 < 3; ++tmp$0) {
      if (tmp$0 == 0)
        if (!(a == 3)) {
          {
            tmp$1 = a = 10;
          }
          break;
        }
      if (tmp$0 == 1)
        if (!(a == 4)) {
          {
            tmp$1 = a = 20;
          }
          break;
        }
      if (tmp$0 == 2) {
        tmp$1 = a = 30;
      }
    }
    tmp$1;
    return a == 10;
  }
}
}, {});
foo.initialize();
