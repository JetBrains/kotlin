var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$3;
    var tmp$2;
    var tmp$1;
    var tmp$0;
    var c = 3;
    var d = 5;
    var z = 0;
    for (tmp$0 = 0; tmp$0 < 2; ++tmp$0) {
      if (tmp$0 == 0)
        if (c == 5 || c == 3) {
          tmp$1 = z++;
          break;
        }
      if (tmp$0 == 1) {
        tmp$1 = z = -1000;
      }
    }
    tmp$1;
    for (tmp$2 = 0; tmp$2 < 2; ++tmp$2) {
      if (tmp$2 == 0)
        if (d == 5 || d == 3) {
          tmp$3 = z++;
          break;
        }
      if (tmp$2 == 1) {
        tmp$3 = z = -1000;
      }
    }
    tmp$3;
    return z;
  }
}
}, {});
foo.initialize();
