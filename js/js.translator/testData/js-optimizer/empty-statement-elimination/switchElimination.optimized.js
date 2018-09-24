var log = '';

function test1(v) {
    switch (v) {
        case 1:
        case 2:
            log += 'A';
            break;
    }
    log += 1;
}

function test2(v) {
    loop: while(--v) {
        switch (v) {
            case 1:
            case 2:
                log += 'B';
                break loop
        }
        log += 2;
    }
}

function test3(v) {
}

function test4(v) {
    log += 'D';
    log += 4;
}

function test5(v) {
    loop: while (--v) {
        log += 'E';
        break loop;
    }
}

function test6(v) {
    log += 'F' + v;
}

function box() {
    test1(1);
    test1(3);
    test2(4);
    test4(3);
    test5(3);
    test6(6);

    return log === 'A112BD4EF6' ? 'OK' : 'fail: ' + log;
}