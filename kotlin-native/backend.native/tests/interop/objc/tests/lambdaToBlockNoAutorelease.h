int callLambdaAsBlock(int (^block)(void)) {
    return block();
}