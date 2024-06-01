debug_param=
if [[ $1 == debug ]]; then
    debug_param="-s -S"
fi


qemu-system-arm -machine mps3-an547 -nographic -kernel ~/zephyrproject/zephyr/build/zephyr/zephyr.elf $debug_param
