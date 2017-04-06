package codes.chia7712.assistant.jmh;

import java.util.Arrays;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
public class HBASE_17233 {

  private final byte[] b = new byte[10000];
  private final byte[] res1 = new byte[10000];

  @Setup
  public void setup() {
  }

  @TearDown
  public void teardown() {

  }

  @Benchmark
  public void arrayCopy() {
    byte[] res = new byte[990];
    System.arraycopy(b, 10, res, 0, 990);
  }

  @Benchmark
  public void copyRange() {
    Arrays.copyOfRange(res1, 10, 1000);
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder().include(HBASE_17233.class.getSimpleName()).warmupIterations(1)
            .measurementIterations(2).forks(1).build();
    new Runner(opt).run();
  }
}
