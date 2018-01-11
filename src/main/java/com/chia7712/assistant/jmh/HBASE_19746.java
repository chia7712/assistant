package com.chia7712.assistant.jmh;

import java.util.stream.Stream;
import org.apache.hadoop.hbase.KeyValue;
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
public class HBASE_19746 {
  private final byte code = KeyValue.Type.Put.getCode();

  @Setup
  public void setup() {
  }

  @TearDown
  public void teardown() {

  }

  @Benchmark
  public KeyValue.Type testLoop() {
    for (KeyValue.Type type : KeyValue.Type.values()) {
      if (type.getCode() == code) {
        return type;
      }
    }
    throw new UnsupportedOperationException();
  }

  @Benchmark
  public KeyValue.Type testSwitch() {
    return KeyValue.Type.codeToType(code);
  }

  @Benchmark
  public KeyValue.Type testStrean() {
    return Stream.of(KeyValue.Type.values())
      .filter(type -> type.getCode() == code)
      .findAny()
      .orElseThrow(() -> new UnsupportedOperationException());
  }


  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(HBASE_19746.class.getSimpleName())
      .warmupIterations(1)
      .measurementIterations(3)
      .forks(2)
      .build();
    new Runner(opt).run();
  }
}
