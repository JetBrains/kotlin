var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$5;
    var tmp$4;
    var tmp$3;
    var tmp$2;
    var tmp$1;
    var tmp$0;
    for (tmp$0 = 0; tmp$0 < 3; ++tmp$0) {
      if (tmp$0 == 0)
        if (3 == 3) {
          tmp$1 = 1;
          break;
        }
      if (tmp$0 == 1)
        if (3 == 2) {
          tmp$1 = 100;
          break;
        }
      if (tmp$0 == 2)
        tmp$1 = 100;
    }
    for (tmp$2 = 0; tmp$2 < 2; ++tmp$2) {
      if (tmp$2 == 0)
        if (2 == 1) {
          tmp$3 = 100;
          break;
        }
      if (tmp$2 == 1)
        tmp$3 = 1;
    }
    for (tmp$4 = 0; tmp$4 < 3; ++tmp$4) {
      if (tmp$4 == 0)
        if (0 == 1) {
          if (true)
            tmp$5 = 100;
          else 
            tmp$5 = 100;
          break;
        }
      if (tmp$4 == 1)
        if (0 == 0) {
          if (false) {
            tmp$5 = 100;
          }
           else {
            tmp$5 = 1;
          }
          break;
        }
      if (tmp$4 == 2)
        tmp$5 = 100;
    }
    var c = tmp$1 + tmp$3 + tmp$5;
    return c == 3;
  }
}
}, {});
foo.initialize();
