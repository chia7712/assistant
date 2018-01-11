package com.chia7712.assistant.jmh;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
public class PerfConcurrentMap {
  private final ConcurrentNavigableMap<String, String> map = new ConcurrentSkipListMap<>();
  private final String first = String.valueOf(1);
  @Setup
  public void setup() {
    for (int i = 0; i != 300000; ++i) {
      String s = String.valueOf(i);
      map.put(s, s);
    }
  }

  @TearDown
  public void teardown() {
    map.clear();
  }

  @Benchmark
  public void values() {
    int size = 0;
    for (String s : map.values()) {
      size += s.length();
    }
  }

  @Benchmark
  public void subValues() {
    int size = 0;
    for (String s : map.tailMap(first).values()) {
      size += s.length();
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
            .include(PerfConcurrentMap.class.getSimpleName())
            .warmupIterations(1)
            .measurementIterations(3)
            .forks(2)
            .build();
    new Runner(opt).run();
  }
}
