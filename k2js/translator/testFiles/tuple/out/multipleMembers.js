var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    if ([1, 2, 3][0] != 1)
      return false;
    if (['a', 'b'][1] != 'b')
      return false;
    var x = ['1', 2, '3', 4, '5', 6];
    if (x[0] != '1')
      return false;
    if (x[1] != 2)
      return false;
    if (x[1] != 2)
      return false;
    if (x[5] != 6)
      return false;
    if (x[4] != '5')
      return false;
    if (x[2] != '3')
      return false;
    if ([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1][20] != 1)
      return false;
    return true;
  }
}
}, {});
foo.initialize();
