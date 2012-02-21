var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(x, y){
    this.$x = x;
    this.$y = y;
  }
  , get_x:function(){
    return this.$x;
  }
  , get_y:function(){
    return this.$y;
  }
  });
  return {Point:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var answer = Anonymous.apply(new Anonymous.Point(3, 5), function(scalar){
      {
        return new Anonymous.Point(this.get_x() * scalar, this.get_y() * scalar);
      }
    }
    );
    if (answer.get_x() == 6 && answer.get_y() == 10)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'FAIL';
    return tmp$0;
  }
}
, apply:function(arg, f){
  {
    return f.call(arg, 2);
  }
}
}, {Point:classes.Point});
Anonymous.initialize();
