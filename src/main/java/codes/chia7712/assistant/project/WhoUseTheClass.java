package codes.chia7712.assistant.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.apache.commons.math3.util.Pair;

public class WhoUseTheClass {

  private static final Collection<Predicate<File>> PREDICATES = Arrays.asList(
          (f) -> !f.getAbsolutePath().contains("generated"),
          (f) -> f.getName().endsWith("java")
  );
  private static final String ROOT_PATH = "/home/chia7712/apache/hbase/";
  private static final int THREADS = 10;

  public static void main(String[] args) throws InterruptedException {
    BlockingQueue<File> folders = new LinkedBlockingQueue<>();
    folders.add(new File(ROOT_PATH));
    Set<Code> javaFiles = new ConcurrentSkipListSet<>();
    Set<Code> testFiles = new ConcurrentSkipListSet<>();
    ExecutorService service = Executors.newFixedThreadPool(THREADS);
    IntStream.range(0, THREADS).forEach(i -> service.execute(new CodeLoader(
            folders,
            code -> {
              if (code.getName().contains("Test")
                || code.getFile().getAbsolutePath().contains("test")) {
                testFiles.add(code);
              } else {
                javaFiles.add(code);
              }
            })));
    service.shutdown();
    service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    System.out.println("Files:" + javaFiles.size()
            + ", test files:" + testFiles.size());
    ConcurrentMap<Code, List<Code>> orphans = new ConcurrentSkipListMap<>();
    ExecutorService serviceV2 = Executors.newFixedThreadPool(THREADS);
    BlockingQueue<Code> javaFilesCopy = new ArrayBlockingQueue<>(javaFiles.size());
    javaFilesCopy.addAll(javaFiles);
    IntStream.range(0, THREADS).forEach(i -> serviceV2.execute(() -> {
      Code orphan;
      while ((orphan = javaFilesCopy.poll()) != null) {
        boolean hasBrother = false;
        for (Code other : javaFiles) {
          if (orphan == other) {
            continue;
          }
          if (other.getContent().contains(orphan.getName())) {
            hasBrother = true;
            break;
          }
        }
        if (!hasBrother) {
          List<Code> tests = new ArrayList<>();
          for (Code test : testFiles) {
            if (test.getContent().contains(orphan.getName())) {
              tests.add(test);
            }
          }
          orphans.put(orphan, tests);
        }
      }
    }));
    serviceV2.shutdown();
    serviceV2.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    orphans.forEach((v, k) -> System.out.println("f:" + v
            + ", tests:" + k));
  }

  private static final class CodeLoader implements Runnable {

    private final BlockingQueue<File> folders;
    private final Consumer<Code> saver;

    CodeLoader(BlockingQueue<File> folder, Consumer<Code> saver) {
      this.folders = folder;
      this.saver = saver;
    }

    @Override
    public void run() {
      File folder;
      while ((folder = folders.poll()) != null) {
        assert folder.isDirectory();
        for (File f : folder.listFiles()) {
          if (f.isDirectory()) {
            folders.add(f);
          }
          if (f.isFile()) {
            if (PREDICATES.stream().allMatch(v -> v.test(f))) {
              Code code = new Code(f);
              saver.accept(code);
            }
          }
        }
      }
    }

  }

  private static final class Code implements Comparable<Code> {

    private final File f;
    private final String content;
    private final File folder;
    private final String name;
    private final String extend;

    Code(final File f) {
      if (f.isFile()) {
        this.f = f;
        this.content = readContent(f);
        this.folder = f.getParentFile();
        Pair<String, String> names = parseName(f);
        this.name = names.getKey();
        this.extend = names.getValue();
      } else {
        throw new RuntimeException("The " + f + " is not file");
      }
    }

    String getExtend() {
      return extend;
    }

    String getName() {
      return name;
    }

    String getContent() {
      return content;
    }

    File getFolder() {
      return folder;
    }

    File getFile() {
      return f;
    }

    @Override
    public int compareTo(Code o) {
      return this.f.compareTo(o.getFile());
    }

    @Override
    public String toString() {
      return f.toString();
    }

    private static Pair<String, String> parseName(File f) {
      String full = f.getName();
      int index = full.indexOf(".");
      if (index == -1) {
        return new Pair<>(full, "");
      } else {
        return new Pair<>(full.substring(0, index), full.substring(index + 1));
      }
    }

    private static String readContent(File f) {
      try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
        StringBuilder buf = new StringBuilder((int) f.length());
        String line;
        while ((line = reader.readLine()) != null) {
          buf.append(line)
                  .append("\n");
        }
        return buf.toString();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
