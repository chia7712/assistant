package codes.chia7712.assistant.parser;


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
  private static final File FILE = new File("/Users/chia7712/hbase-chia7712-regionserver-cat04.ncku.edu.tw.log");
  public static void main(String[] args) throws IOException {
    parse("[IKEA]", Arrays.asList(
      new Grabber("read", Long::valueOf),
      new Grabber("write", Long::valueOf),
      new Grabber("count", Long::valueOf)
    ));
    parse("[CAT]", Arrays.asList(
      new Grabber("readTime", Long::valueOf),
      new Grabber("peekTime", Long::valueOf),
      new Grabber("timeLimitTime", Long::valueOf),
      new Grabber("checkTime", Long::valueOf),
      new Grabber("matchTime", Long::valueOf),
      new Grabber("optimizeTime", Long::valueOf),
      new Grabber("overLimitTime", Long::valueOf),
      new Grabber("rowMayTime", Long::valueOf),
      new Grabber("filterAndLimitTime", Long::valueOf),
      new Grabber("addCellTime", Long::valueOf),
      new Grabber("updateSizeTime", Long::valueOf),
      new Grabber("nextCellTime", Long::valueOf),
      new Grabber("setSizeTime", Long::valueOf),
      new Grabber("doneTime", Long::valueOf),
      new Grabber("callCount", Long::valueOf),
      new Grabber("loopCount", Long::valueOf)
    ));
  }
  private static void parse(String prefix, List<Grabber> grabbers) throws IOException {
    long count = 0;
    Map<Grabber, List<Long>> result = new LinkedHashMap<>();
    grabbers.forEach(v -> result.put(v, new ArrayList<>()));
    try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(prefix)) {
          ++count;
          final String s = line;
          result.forEach((k, v) -> v.add(findValue(s, k)));
        }
      }
    }
    StringBuilder buf = new StringBuilder();
    result.forEach((v, k) -> buf.append(v.getName()).append("\t"));
    buf.append("count").append("\n");
    result.forEach((v, k) -> buf.append(k.stream().mapToLong(t -> t).sum()).append("\t"));
    buf.append(count).append("\n");
    System.out.println(buf);
  }
  private static class Grabber implements Function<String, Long>, Comparable<Grabber> {
    private final Function<String, Long> f;
    private final String name;
    Grabber(String name, Function<String, Long> f) {
      this.name = name;
      this.f = f;
    }
    String getName() {
      return name;
    }
    @Override
    public Long apply(String t) {
      return f.apply(t);
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
