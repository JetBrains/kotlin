var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
  $d = function(a){
    {
      return a + 1;
    }
  }
  ;
  $p = function(a){
    {
      return a * 3;
    }
  }
  ;
  $list = new Kotlin.ArrayList;
}
, get_d:function(){
  return $d;
}
, get_p:function(){
  return $p;
}
, get_list:function(){
  return $list;
}
, chain:function(start){
  {
    var tmp$0;
    var res = start;
    {
      tmp$0 = foo.get_list().iterator();
      while (tmp$0.hasNext()) {
        var func = tmp$0.next();
        {
          res = func(res);
        }
      }
    }
    return res;
  }
}
, box:function(){
  {
    if (foo.chain(0) != 0) {
      return false;
    }
    foo.get_list().add(foo.get_d());
    if (foo.get_list().get(0)(0) != 1) {
      return false;
    }
    foo.get_list().add(foo.get_p());
    if (foo.get_list().get(1)(10) != 30) {
      return false;
    }
    if (foo.chain(0) != 3) {
      return false;
    }
    foo.get_list().add(function(it){
      {
        return it * it;
      }
    }
    );
    foo.get_list().add(function(it){
      {
        return it - 100;
      }
    }
    );
    if (foo.chain(2) != -19) {
      return false;
    }
    if (function(a){
      {
        return a * a;
      }
    }
    (3) != 9) {
      return false;
    }
    return true;
  }
}
}, {});
foo.initialize();
