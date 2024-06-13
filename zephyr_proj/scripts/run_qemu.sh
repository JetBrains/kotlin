debug_param=
if [[ $1 == debug ]]; then
    debug_param="-s -S"
fi


~/qemu/build/qemu-system-arm -machine mps2-an385 -nographic -kernel ~/zephyrproject/zephyr/build/zephyr/zephyr.elf $debug_param