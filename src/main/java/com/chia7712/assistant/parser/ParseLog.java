package com.chia7712.assistant.parser;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ParseLog {
  private static final String STOP_WORD = ",";
  private static final String HOME = System.getProperty("user.home");
  private static final File FILE = new File(HOME + "/Dropbox/hbase-chia7712-regionserver-cat06.ncku.edu.tw.log");
  public static void main(String[] args) throws IOException {
    parse("[IKEA]", Arrays.asList(
      new Grabber("read", Long::valueOf),
      new Grabber("write", Long::valueOf)
    ));
    parse("[TINA]", Arrays.asList(
      new Grabber("createScanner", Long::valueOf, true)
    ));
    parse("[CAT]", Arrays.asList(
      new Grabber("readTime", Long::valueOf),
      new Grabber("peekTime", Long::valueOf),
      new Grabber("matchTime", Long::valueOf, true),
      new Grabber("updateSizeTime", Long::valueOf, true),
      new Grabber("nextCellTime", Long::valueOf, true)
    ));
  }
  private static void parse(String prefix, List<Grabber> grabbers) throws IOException {
    Map<Grabber, List<Long>> result = new LinkedHashMap<>();
    grabbers.forEach(v -> result.put(v, new ArrayList<>()));
    try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(prefix)) {
          final String s = line;
          result.forEach((k, v) -> v.add(findValue(s, k)));
        }
      }
    }
    StringBuilder buf = new StringBuilder();
    result.forEach((v, k) -> {
      if (v.getFindMinAndMax()) {
        buf.append(k.stream().mapToLong(t -> t).min().getAsLong()).append("\t")
            .append(k.stream().mapToLong(t -> t).average().getAsDouble()).append("\t")
            .append(k.stream().mapToLong(t -> t).max().getAsLong()).append("\t");
      } else {
        buf.append(k.stream().mapToLong(t -> t).sum()).append("\t");
      }
    });
    buf.deleteCharAt(buf.length() - 1);
    System.out.println(buf);
  }
  private static class Grabber implements Function<String, Long>, Comparable<Grabber> {
    private final Function<String, Long> f;
    private final String name;
    private final boolean findMinAndMax;
    Grabber(String name, Function<String, Long> f) {
      this(name, f, false);
    }
    Grabber(String name, Function<String, Long> f, boolean findMinAndMax) {
      this.name = name;
      this.f = f;
      this.findMinAndMax = findMinAndMax;
    }
    boolean getFindMinAndMax() {
      return this.findMinAndMax;
    }
    String getName() {
      return name;
    }
    @Override
    public Long apply(String t) {
      return f.apply(t) / 1000000;
    }

    @Override
    public int compareTo(Grabber o) {
      return name.compareTo(o.name);
    }
    
  }

  private static Long findValue(String line, Grabber grabber) {
    String key = grabber.getName() + ":";
    int index = line.indexOf(key);
    if (index == -1) {
      throw new RuntimeException("Why? " + grabber.getName());
    }
    int last = line.indexOf(STOP_WORD, index + key.length());
    int lastIndex = last == -1 ? line.length() : last;
    return grabber.apply(line.substring(index + key.length(), lastIndex));
  }
}
