var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var i = 0;
    var arr = new Kotlin.ArrayList;
    while (i++ < 10) {
      arr.add(i);
    }
    var sum = 0;
    {
      tmp$0 = arr.iterator();
      while (tmp$0.hasNext()) {
        var a = tmp$0.next();
        {
          sum += a;
        }
      }
    }
    return sum == 55 && arr.size() == 10;
  }
}
}, {});
foo.initialize();
