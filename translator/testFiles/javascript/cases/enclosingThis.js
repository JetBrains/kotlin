{
    classes = function () {
        var Point = Class.create({initialize:function (x, y) {
            this.$x = x;
            this.$y = y;
        }, get_x:function () {
            return this.$x;
        }, get_y:function () {
            return this.$y;
        }, mul:function () {
            return (that = this, function (scalar) {
                return new Point(that.get_x() * scalar, that.get_y() * scalar);
            })
                ;
        }
        });
        return {Point:Point};
    }
        ();
    Anonymous = Namespace.create({initialize:function () {
        Anonymous.$m = (new Anonymous.Point(2, 3)).mul();
    }, get_m:function () {
        return Anonymous.$m;
    }, box:function () {
        var answer = Anonymous.get_m()(5);
        return answer.get_x() === 10 && answer.get_y() === 15 ? 'OK' : 'FAIL';
    }
    }, classes);
    Anonymous.initialize();
}
