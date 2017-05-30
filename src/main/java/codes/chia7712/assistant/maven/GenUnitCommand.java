package codes.chia7712.assistant.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class GenUnitCommand {

  private static final String EXTRA_OPTS = null;
  private static final String BRANCH = "master";
  private static final String ISSUE = "1.2.6";
  private static final int PARALLER = 1;
  private static final String HOME = System.getProperty("user.home");
  private static final String PATH = HOME + "/Dropbox/hbase-jira/" + ISSUE + "/unittest"
    + ("master".equals(BRANCH) ? "" : "_" + BRANCH); 
  private static final boolean ALL_TEST = true;

  public static void main(String[] args) throws IOException {
    List<TestFileResult> results = new ArrayList<>(100);
    File dir = new File(PATH);
    if (dir.exists()) {
      for (File f : dir.listFiles()) {
        results.addAll(parse(f));
      }
      if (results.isEmpty()) {
        System.out.println("No found of unittest file from the dir:" + PATH);
      }
    } else {
      System.out.println("No found of unittest dir:" + PATH);
    }
    Set<TestFileResult> successes = new TreeSet<>();
    Set<TestFileResult> failures = new TreeSet<>();
    Set<TestFileResult> errors = new TreeSet<>();
    Set<TestFileResult> skipped = new TreeSet<>();
    results.forEach(result -> {
      if (result.numberOfErrors != 0) {
        errors.add(result);
      }
      if (result.numberOfFailures != 0) {
        failures.add(result);
      }
      if (result.numberOfSkipped != 0) {
        skipped.add(result);
      }
      if (result.numberOfFailures == 0 && result.numberOfErrors == 0) {
        successes.add(result);
      } 
    });
    System.out.println("success:" + successes.size());
    System.out.println("failures:" + failures.size());
    System.out.println("errors:" + errors.size());
    System.out.println("skipped:" + skipped.size());
    System.out.println("----------------------");
    System.out.println(generate(successes));
  }

  public static String generate(Set<TestFileResult> results) throws IOException {
    StringBuilder builder = new StringBuilder("mvn clean test -fae -Dtest.exclude.pattern=");
    results.forEach(r -> builder.append("**/").append(r.testClass).append(".java,"));
    builder.deleteCharAt(builder.length() - 1);
    builder.append(" -DsecondPartForkCount=")
            .append(PARALLER);
    if (ALL_TEST) {
      builder.append(" -PrunAllTests");
    }
    if (EXTRA_OPTS != null && EXTRA_OPTS.length() != 0) {
      builder.append(" ")
              .append(EXTRA_OPTS);
    }
    builder.append(" | tee ~/test_")
            .append(ISSUE);
    return builder.substring(0, builder.length());
  }

  private static List<TestFileResult> parse(final File f) throws IOException {
    List<TestFileResult> results = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.startsWith("Tests run")) {
          continue;
        }
        parse(line).ifPresent(results::add);
      }
    }
    return results;
  }
  private static Optional<TestFileResult> parse(String line) {
    final String[] elements = line.split(",");
    if (elements.length != 5) {
      return Optional.empty();
    }
    int numberOfUts = findElements(elements[0]);
    int numberOfFailures = findElements(elements[1]);
    int numberOfErrors = findElements(elements[2]);
    int numberOfSkipped = findElements(elements[3]);
    final String[] timeAndTestClass = elements[4].split("-");
    double elapsed = findElapsed(timeAndTestClass[0]);
    String testClass = findTestClass(timeAndTestClass[1]);
    return Optional.of(new TestFileResult(testClass, elapsed, numberOfUts, numberOfFailures, numberOfErrors, numberOfSkipped));
  }
  private static double findElapsed(String line) {
    return Double.valueOf(line.substring(line.indexOf(":") + 2, line.length() - 5));
  }
  private static String findTestClass(String line) {
    return line.substring(4);
  }
  private static int findElements(String line) {
    return Integer.valueOf(line.substring(line.indexOf(":") + 2));
  }
  private static class TestFileResult implements Comparable<TestFileResult> {
    private final String testClass;
    private final double elapsed;
    private final int numberOfUts;
    private final int numberOfFailures;
    private final int numberOfErrors;
    private final int numberOfSkipped;
    TestFileResult(final String testClass, double elapsed, int numberOfUts, int numberOfFailures,
      int numberOfErrors, int numberOfSkipped) {
      this.testClass = testClass;
      this.elapsed = elapsed;
      this.numberOfUts = numberOfUts;
      this.numberOfFailures = numberOfFailures;
      this.numberOfErrors = numberOfErrors;
      this.numberOfSkipped = numberOfSkipped;
    }

    @Override
    public int compareTo(TestFileResult o) {
      return testClass.compareTo(o.testClass);
    }
  }
}
