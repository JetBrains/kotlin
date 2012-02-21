var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$m = 0;
  }
  , get_m:function(){
    return this.$m;
  }
  , set_m:function(tmp$0){
    this.$m = tmp$0;
  }
  , eval_0:function(){
    {
      var tmp$0_0;
      var d = (tmp$0_0 = this , function(){
        {
          var tmp$0;
          var c = (tmp$0 = tmp$0_0 , function(){
            {
              return this + 3;
            }
          }
          );
          tmp$0_0.set_m(tmp$0_0.get_m() + c.call(3));
        }
      }
      );
      d();
    }
  }
  });
  return {M:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var a = new foo.M;
    if (a.get_m() != 0)
      return false;
    a.eval_0();
    if (a.get_m() != 6)
      return false;
    return true;
  }
}
}, {M:classes.M});
foo.initialize();
