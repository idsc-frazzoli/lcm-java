// code by jph
package lcm.logging;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import ch.ethz.idsc.lcm.test.BinaryBlob;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Range;
import junit.framework.TestCase;
import lcm.logging.Log.Event;

public class LogTest extends TestCase {
  public void testSimple() {
    assertEquals(Log.LOG_MAGIC, 0xEDA1DA01);
  }

  public void testEvents() throws Exception {
    URL url = Log.class.getResource("/log/lcmlog-2017-08-24.04");
    String filename = url.getFile();
    Log log = new Log(filename, "r");
    int count = 0;
    Tensor range = Tensors.empty();
    List<Integer> lengths = Arrays.asList(1206, 512);
    try {
      while (true) {
        Event event = log.readNext();
        range.append(RealScalar.of(event.eventNumber));
        BinaryBlob binaryBlob = new BinaryBlob(event.data);
        assertTrue(lengths.contains(binaryBlob.data_length));
        assertEquals(binaryBlob.data.length, binaryBlob.data_length);
        ++count;
      }
    } catch (Exception exception) {
      // ---
    }
    assertEquals(range, Range.of(0, range.length()));
    assertEquals(count, 34);
  }
}
