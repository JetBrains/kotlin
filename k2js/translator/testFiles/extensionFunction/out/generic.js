var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, findAll:function(receiver, predicate){
  {
    var tmp$0;
    var result = new Kotlin.ArrayList;
    {
      tmp$0 = receiver.iterator();
      while (tmp$0.hasNext()) {
        var t = tmp$0.next();
        {
          if (predicate(t))
            result.add(t);
        }
      }
    }
    return result;
  }
}
, box:function(){
  {
    var tmp$0;
    var list = new Kotlin.ArrayList;
    list.add(2);
    list.add(3);
    list.add(5);
    var m = foo.findAll(list, function(name_0){
      {
        return name_0 < 4;
      }
    }
    );
    if (m.size() == 2)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
}, {});
foo.initialize();
