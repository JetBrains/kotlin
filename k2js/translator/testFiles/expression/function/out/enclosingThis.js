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
  , mul:function(){
    {
      var tmp$0;
      return tmp$0 = this , function(scalar){
        {
          return new Anonymous.Point(tmp$0.get_x() * scalar, tmp$0.get_y() * scalar);
        }
      }
      ;
    }
  }
  });
  return {Point:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
  $m = (new Anonymous.Point(2, 3)).mul();
}
, get_m:function(){
  return $m;
}
, box:function(){
  {
    var tmp$0;
    var answer = Anonymous.get_m()(5);
    if (answer.get_x() == 10 && answer.get_y() == 15)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'FAIL';
    return tmp$0;
  }
}
}, {Point:classes.Point});
Anonymous.initialize();
