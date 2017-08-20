// code by lcm
package lcm.spy;

enum StaticHelper {
  ;
  static void spyUsage() {
    System.err.println("usage: lcm-spy [options]");
    System.err.println("");
    System.err.println("lcm-spy is the Lightweight Communications and Marshalling traffic ");
    System.err.println("inspection utility.  It is a graphical tool for viewing messages received on ");
    System.err.println("an LCM network, and is analagous to tools like Ethereal/Wireshark and tcpdump");
    System.err.println("in that it is able to inspect all LCM messages received and provide information");
    System.err.println("and statistics on the channels used.");
    System.err.println("");
    System.err.println("When given appropriate LCM type definitions, lcm-spy is able to");
    System.err.println("automatically detect and decode messages, and can display the individual fields");
    System.err.println("of recognized messages.  lcm-spy is limited to displaying statistics for");
    System.err.println("unrecognized messages.");
    System.err.println("");
    System.err.println("Options:");
    System.err.println("  -l, --lcm-url=URL      Use the specified LCM URL");
    System.err.println("  -h, --help             Shows this help text and exits");
    System.err.println("");
    System.exit(1);
  }
}
