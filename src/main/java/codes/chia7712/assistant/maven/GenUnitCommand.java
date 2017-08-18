package codes.chia7712.assistant.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GenUnitCommand {

  private static final List<String> SKIPPED_CLASSES = Arrays.asList(
          "org.apache.hadoop.hbase.http.TestSpnegoHttpServer",
          "org.apache.hadoop.hbase.master.TestMasterFailover",
          "org.apache.hadoop.hbase.master.assignment.TestAssignmentManager",
          "org.apache.hadoop.hbase.util.TestPoolMap",
          "org.apache.hadoop.hbase.quotas.TestQuotaThrottle",
          "org.apache.hadoop.hbase.rest.client.TestXmlParsing",
          "org.apache.hadoop.hbase.client.TestClientClusterStatus",
          "org.apache.hadoop.hbase.client.TestMultiParallel",
          "org.apache.hadoop.hbase.regionserver.TestCompactionInDeadRegionServer"
  );
  private static final List<TestFileResult> SKIPPED_RESULTS
          = SKIPPED_CLASSES.stream().map(TestFileResult::new).collect(Collectors.toList());

  private static final String EXTRA_OPTS = null;
  private static final String BRANCH = "master";
  private static final String ISSUE = "18503";
  private static final int PARALLER = 1;
  private static final String HOME = System.getProperty("user.home");
  private static final String PATH = HOME + "/Dropbox/hbase-jira/" + ISSUE + "/unittest"
          + ("master".equals(BRANCH) ? "" : "_" + BRANCH);
  private static final boolean ALL_TEST = true;
  private static final boolean DISABLE_COLOR = true;
  public static void main(String[] args) throws IOException {
    List<TestFileResult> results = new ArrayList<>(100);
    File dir = new File(PATH);
    if (dir.exists()) {
      for (File f : dir.listFiles()) {
        results.addAll(parse(f));
      }
    } else {
      System.out.println("No found of unittest dir:" + PATH);
    }
    results.addAll(SKIPPED_RESULTS);
    Set<TestFileResult> successes = new TreeSet<>();
    Set<TestFileResult> failures = new TreeSet<>();
    Set<TestFileResult> errors = new TreeSet<>();
    Set<TestFileResult> skipped = new TreeSet<>();
    Set<TestFileResult> neverSucceed = new TreeSet<>();
    results.forEach(result -> {
      if (result.getNumberOfErrors() != 0) {
        errors.add(result);
      }
      if (result.getNumberOfFailures() != 0) {
        failures.add(result);
      }
      if (result.getNumberOfSkipped() != 0) {
        skipped.add(result);
      }
      if (result.getNumberOfFailures() == 0 && result.getNumberOfErrors() == 0) {
        successes.add(result);
      }
    });
    failures.stream().filter(v -> !successes.contains(v)).forEach(neverSucceed::add);
    errors.stream().filter(v -> !successes.contains(v)).forEach(neverSucceed::add);
    System.out.println("succeed classes:" + successes.size());
    System.out.println("succeed UTs:" + successes.stream().mapToInt(v -> v.getNumberOfUts()).sum());
    successes.forEach(v -> System.out.println(v.getTestClass()));
    System.out.println("failed UTs:" + failures.stream().mapToInt(v -> v.getNumberOfFailures()).sum());
    failures.forEach(v -> System.out.println(v.getTestClass()));
    System.out.println("erroneous UTs:" + errors.stream().mapToInt(v -> v.getNumberOfErrors()).sum());
    errors.forEach(v -> System.out.println(v.getTestClass()));
    System.out.println("skipped UTs:" + skipped.stream().mapToInt(v -> v.getNumberOfSkipped()).sum());
    skipped.forEach(v -> System.out.println(v.getTestClass()));
    System.out.println("neverSucceed UTs:" + neverSucceed.stream().mapToInt(v -> v.getNumberOfErrors() + v.getNumberOfFailures()).sum());
    neverSucceed.forEach(v -> System.out.println(v.getTestClass()));
    System.out.println("\n----------------------[Exclude succeed tests]----------------------");
    System.out.println(generateGeneralCommand(successes));
    System.out.println("\n----------------------[Exclude succeed/failed tests]---------------------");
    Set<TestFileResult> succeedAndFailed = new TreeSet<>();
    succeedAndFailed.addAll(successes);
    succeedAndFailed.addAll(neverSucceed);
    System.out.println(generateGeneralCommand(succeedAndFailed));
    System.out.println("\n----------------------[Failed tests]----------------------");
    generateSeparateCommand(neverSucceed).forEach(System.out::println);
    System.out.println("\n----------------------[All failed tests]----------------------");
    System.out.println(generateSeparateCommandInSingle(neverSucceed));
  }

  private static List<String> generateSeparateCommand(Set<TestFileResult> results) {
    return results.stream()
      .map(r -> "mvn clean test -Dtest=" + r.getTestClass() + " -P skipSparkTests " + (DISABLE_COLOR ? "-B" : "")
              + " | tee ~/test_" + r.getSimpleTestClass())
      .collect(Collectors.toList());
  }

  private static String generateSeparateCommandInSingle(Set<TestFileResult> results) {
    StringBuilder builder = new StringBuilder(300);
    builder.append("mvn clean test -Dtest=\"");
    results.forEach(v -> {
      builder.append(v.getTestClass())
             .append(",");
    });
    if (!results.isEmpty()) {
      builder.deleteCharAt(builder.length() - 1);
    }
    builder.append("\" -P skipSparkTests | tee ~/test_")
           .append(ISSUE);
    return builder.toString();
  }

  private static String generateGeneralCommand(Set<TestFileResult> results) {
    StringBuilder builder = new StringBuilder("mvn clean test -fae -Dtest.exclude.pattern=");
    results.forEach(r -> builder.append("**/*").append(r.getSimpleTestClass()).append(".java,"));
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
    builder.append(DISABLE_COLOR ? " -B" : "")
           .append(" | tee ~/test")
           .append("master".equals(BRANCH) ? "" : "_" + BRANCH)
           .append("_")
           .append(ISSUE);
    return builder.substring(0, builder.length());
  }

  private static List<TestFileResult> parse(final File f) throws IOException {
    List<TestFileResult> results = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.contains("Tests run:")) {
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
    String key1 = "Time elapsed: ";
    String key2 = " s";
    final int indexOfKey1 = line.indexOf(key1);
    final int indexOfKey2 = line.indexOf(key2, key1.length() + indexOfKey1);
    return Double.valueOf(line.substring(indexOfKey1 + key1.length(), indexOfKey2));
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

    TestFileResult(final String testClass) {
      this(testClass, 0, 0, 0, 0, 0);
    }

    TestFileResult(final String testClass, double elapsed, int numberOfUts, int numberOfFailures,
            int numberOfErrors, int numberOfSkipped) {
      this.testClass = testClass;
      this.elapsed = elapsed;
      this.numberOfUts = numberOfUts;
      this.numberOfFailures = numberOfFailures;
      this.numberOfErrors = numberOfErrors;
      this.numberOfSkipped = numberOfSkipped;
    }

    String getSimpleTestClass() {
      return testClass.substring(testClass.lastIndexOf(".") + 1);
    }

    String getTestClass() {
      return testClass;
    }

    double getElapsed() {
      return elapsed;
    }

    int getNumberOfUts() {
      return numberOfUts;
    }

    int getNumberOfFailures() {
      return numberOfFailures;
    }

    int getNumberOfErrors() {
      return numberOfErrors;
    }

    int getNumberOfSkipped() {
      return numberOfSkipped;
    }

    @Override
    public int compareTo(TestFileResult o) {
      return testClass.compareTo(o.testClass);
    }
  }
}
