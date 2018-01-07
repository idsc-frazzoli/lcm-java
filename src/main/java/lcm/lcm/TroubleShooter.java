// code by jph
package lcm.lcm;

/* package */ enum TroubleShooter {
  ;
  public static String joinGroup() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("=== TROUBLESHOOT ===\n");
    stringBuilder.append("on LINUX: in the console run the two commands\n");
    stringBuilder.append("sudo ifconfig lo multicast\n");
    stringBuilder.append("sudo route add -net 224.0.0.0 netmask 240.0.0.0 dev lo\n");
    stringBuilder.append("\n");
    // according to andi, joingroup fails on a mac when using wifi
    // when not using -Djava.net.preferIPv4Stack=true as VM argument
    stringBuilder.append("on MAC: if you use wifi then add the VM argument\n");
    stringBuilder.append("-Djava.net.preferIPv4Stack=true\n");
    return stringBuilder.toString();
  }
}
