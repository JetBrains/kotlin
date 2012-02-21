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
    arr.remove(2);
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
    return sum == 52 && arr.get(1) == 2 && arr.get(2) == 4 && arr.get(3) == 5 && arr.get(4) == 6 && arr.get(8) == 10;
  }
}
}, {});
foo.initialize();
