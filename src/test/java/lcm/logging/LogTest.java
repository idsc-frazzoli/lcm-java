// code by jph
package lcm.logging;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import ch.ethz.idsc.lcm.test.BinaryBlob;
import junit.framework.TestCase;
import lcm.logging.Log.Event;

public class LogTest extends TestCase {
  public void testSimple() {
    assertEquals(Log.LOG_MAGIC, 0xEDA1DA01);
  }

  public void testEvents() throws Exception {
    URL url = Log.class.getResource("/log/lcmlog-2017-08-24.04");
    String filename = url.getFile();
    int count = 0;
    List<Long> range = new LinkedList<>();
    List<Integer> lengths = Arrays.asList(1206, 512);
    try (Log log = new Log(filename, "r")) {
      while (true) {
        Event event = log.readNext();
        range.add(event.eventNumber);
        BinaryBlob binaryBlob = new BinaryBlob(event.data);
        assertTrue(lengths.contains(binaryBlob.data_length));
        assertEquals(binaryBlob.data.length, binaryBlob.data_length);
        ++count;
      }
    } catch (Exception exception) {
      // ---
    }
    assertEquals(range, LongStream.range(0, range.size()).boxed().collect(Collectors.toList()));
    assertEquals(count, 34);
  }
}
