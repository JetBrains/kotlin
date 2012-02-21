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
    var oneToFive = new Kotlin.NumberRange(1, 4);
    if (oneToFive.contains(5))
      return false;
    if (oneToFive.contains(0))
      return false;
    if (oneToFive.contains(-100))
      return false;
    if (oneToFive.contains(10))
      return false;
    if (!oneToFive.contains(1))
      return false;
    if (!oneToFive.contains(2))
      return false;
    if (!oneToFive.contains(3))
      return false;
    if (!oneToFive.contains(4))
      return false;
    if (!(oneToFive.get_start() == 1))
      return false;
    if (!(oneToFive.get_size() == 4))
      return false;
    if (!(oneToFive.get_end() == 4))
      return false;
    var sum = 0;
    {
      tmp$0 = oneToFive.iterator();
      while (tmp$0.hasNext()) {
        var i = tmp$0.next();
        {
          sum += i;
        }
      }
    }
    {
      tmp$1 = oneToFive.iterator();
      while (tmp$1.hasNext()) {
        var i$0 = tmp$1.next();
        {
          Kotlin.print(i$0);
        }
      }
    }
    if (sum != 10)
      return false;
    return true;
  }
}
, main:function(){
  {
    foo.box();
  }
}
}, {});
foo.initialize();
