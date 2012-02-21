var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, testSize:function(expectedSize, i){
  {
    return i.length == expectedSize;
  }
}
, testSum:function(expectedSum, i){
  {
    var tmp$0;
    var sum = 0;
    {
      tmp$0 = Kotlin.arrayIterator(i);
      while (tmp$0.hasNext()) {
        var j = tmp$0.next();
        {
          sum += j;
        }
      }
    }
    return expectedSum == sum;
  }
}
, box:function(){
  {
    return foo.testSize(0, []) && foo.testSum(0, []) && foo.testSize(3, [1, 1, 1]) && foo.testSum(3, [1, 1, 1]) && foo.testSize(6, [1, 1, 1, 2, 3, 4]) && foo.testSum(30, [10, 20, 0]);
  }
}
}, {});
foo.initialize();
