function foo() {
    $a: {
        $b: {
            console.log("1");
            console.log(function() {
                $aa: {
                    console.log("2");
                }
            });

            if (condition()) {
                break $a;
            }
            else {
                break $b;
            }
        }
    }

    $c: {
        $d: {
            console.log("1");
        }
        $e: {
            console.log("1");
        }
    }
}