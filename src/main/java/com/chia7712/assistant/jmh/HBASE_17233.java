package com.chia7712.assistant.jmh;

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

  @Benchmark
  public void copyRangeV2() {
    copyOfRangeV2(res1, 10, 1000);
  }

  @Benchmark
  public void copyRangeV3() {
    copyOfRangeV3(res1, 10, 1000);
  }

  @Benchmark
  public void copyRangeV4() {
    copyOfRangeV4(res1, 10, 1000);
  }

  @Benchmark
  public void copyRangeV5() {
    copyOfRangeV5(res1, 10, 1000);
  }

  private static byte[] copyOfRangeV2(byte[] original, int from, int to) {
    int newLength = to - from;
    if (newLength < 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    byte[] copy = new byte[newLength];
    System.arraycopy(original, from, copy, 0,
            Math.min(original.length - from, newLength));
    return copy;
  }

  private static byte[] copyOfRangeV3(byte[] original, int from, int to) {
    byte[] copy = new byte[to - from];
    System.arraycopy(original, from, copy, 0,
            Math.min(original.length - from, to - from));
    return copy;
  }

  private static byte[] copyOfRangeV4(byte[] original, int from, int to) {
    byte[] copy = new byte[to - from];
    System.arraycopy(original, from, copy, 0, to - from);
    return copy;
  }

  private static byte[] copyOfRangeV5(byte[] original, int from, int to) {
    int length = Math.min(original.length - from, to - from);
    byte[] copy = new byte[length];
    System.arraycopy(original, from, copy, 0, length);
    return copy;
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
            .include(HBASE_17233.class.getSimpleName())
            .warmupIterations(1)
            .measurementIterations(3)
            .forks(2)
            .build();
    new Runner(opt).run();
  }
}
