var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
  }
  , f:function(){
    {
      var t = Kotlin.object.create({initialize:function(){
        this.$c = true;
      }
      , get_c:function(){
        return this.$c;
      }
      });
      var z = Kotlin.object.create({initialize:function(){
        this.$c = true;
      }
      , get_c:function(){
        return this.$c;
      }
      });
      return t.get_c() && z.get_c();
    }
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.A).f();
  }
}
}, {A:classes.A});
foo.initialize();
