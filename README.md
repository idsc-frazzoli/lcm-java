# ch.ethz.idsc.lcm-java

<a href="https://travis-ci.org/idsc-frazzoli/jlcm"><img src="https://travis-ci.org/idsc-frazzoli/lcm-java.svg?branch=master" alt="Build Status"></a>

LCM tools in Java.

Original code from `https://lcm-proj.github.io/`

Code adapted in collaboration with `SwissTrolley+`

Compatible with original Java LCM Tools: Spy, LogPlayer, ...

Only modification: Class discovery is narrowed.

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

Remark: On Mac OS when using Wifi it may be necessary to add the following `-Djava.net.preferIPv4Stack=true` into the run configuration  -> arguments -> VM arguments.

## Include in Project

    <dependencies>
        ...
        <dependency>
            <groupId>ch.ethz.idsc</groupId>
            <artifactId>lcm-java</artifactId>
            <version>0.0.2</version>
        </dependency>
    </dependencies>

    <repositories>
        ...
        <repository>
            <id>lcm-java-mvn-repo</id>
            <url>https://raw.github.com/idsc-frazzoli/lcm-java/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>


## References

The library is used in the projects:
* `retina`
* `owly3d`
* `SwissTrolley+`
