foo = {};
(function(foo){
  foo.Test = Class.create({initialize:function(){
  }
  });
  foo.box = function(){
    var test = foo.Test();
    return true;
  }
  ;
}
(foo));
