# ch.ethz.idsc.lcm-java <a href="https://travis-ci.org/idsc-frazzoli/jlcm"><img src="https://travis-ci.org/idsc-frazzoli/lcm-java.svg?branch=master" alt="Build Status"></a>

Enhanced LCM tools in Java, version `0.0.9`

Original code from [lcm-proj.github.io](https://lcm-proj.github.io/)

Code adapted in collaboration with `SwissTrolley+` at IDSC.

The adaptation is compatible with the original Java LCM Tools.
Single exception: the class discovery is narrowed.

Diverse projects rely on the `lcm-java` library:

<table>
<tr>
<td>

![usecase_swisstrolley](https://user-images.githubusercontent.com/4012178/35968228-88547e90-0cc3-11e8-978d-4f822515156f.png)

SwissTrolley plus

<td>

![usecase_gokart](https://user-images.githubusercontent.com/4012178/35968269-a92a3b46-0cc3-11e8-8d5e-1276762cdc36.png)

Autonomous Gokart

</tr>
</table>

## Enhancements

* in Spy: display of total data rate; alignments of columns
* in LogPlayer: file chooser dialog

## General Setup

Quote from [Multicast Setup](https://lcm-proj.github.io/multicast_setup.html):
"If your computer is not connected to any network, you may need to explicitly enable multicast traffic by adding multicast entries to your system's routing table. On *Linux*, you can setup the loopback interface for multicast with the following commands:

    sudo ifconfig lo multicast
    sudo route add -net 224.0.0.0 netmask 240.0.0.0 dev lo

Remember, you must always do this to use LCM if your machine is not connected to any external network."

Remark: On *Mac OS* when using Wifi it may be necessary to add the following to the list of VM arguments

    -Djava.net.preferIPv4Stack=true

## Integration

Specify `repository` and `dependency` of the lcm-java library in the `pom.xml` file of your maven project:

    <dependencies>
        <dependency>
            <groupId>ch.ethz.idsc</groupId>
            <artifactId>lcm-java</artifactId>
            <version>0.0.9</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>lcm-java-mvn-repo</id>
            <url>https://raw.github.com/idsc-frazzoli/lcm-java/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
