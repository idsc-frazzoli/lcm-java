// code adapted by jph
package lcm.lcm;

import java.util.regex.Pattern;

/** the application layer obtains an instance of {@link SubscriptionRecord}
 * when subscribing.
 * 
 * members are deliberately package visibility */
public class SubscriptionRecord {
  final String regex;
  final LCMSubscriber lcsub;
  private final Pattern pat;

  public SubscriptionRecord(String regex, LCMSubscriber lcsub) {
    this.regex = regex;
    this.lcsub = lcsub;
    pat = Pattern.compile(regex);
  }

  boolean matches(String channel) {
    return pat.matcher(channel).matches();
  }
}