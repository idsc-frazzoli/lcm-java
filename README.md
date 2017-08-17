# ch.ethz.idsc.lcm-java

<a href="https://travis-ci.org/idsc-frazzoli/jlcm"><img src="https://travis-ci.org/idsc-frazzoli/lcm-java.svg?branch=master" alt="Build Status"></a>

LCM tools in Java.

Original code from `https://lcm-proj.github.io/`

Code adapted in collaboration with `SwissTrolley+`

100% compatible with original LCM

Version `0.0.2`

## Enhancements in Spy

* display of total data rate
* alignments of columns

## Enhancements in LogPlayer

* file chooser dialog


## General Setup

Quote from [Multicast Setup](https://lcm-proj.github.io/multicast_setup.html):
"If your computer is not connected to any network, you may need to explicitly enable multicast traffic by adding multicast entries to your system's routing table. On Linux, you can setup the loopback interface for multicast with the following commands:

    sudo ifconfig lo multicast
    sudo route add -net 224.0.0.0 netmask 240.0.0.0 dev lo

Remember, you must always do this to use LCM if your machine is not connected to any external network."
