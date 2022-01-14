#!/usr/sbin/dtrace -s

:::GCStart_V1
{
	self->start = timestamp;
}

:::GCEnd_V1
/self->start/
{
	@ = quantize(((timestamp - self->start)));
    self->start = 0;
}

tick-10s
{
	printa(@);
}