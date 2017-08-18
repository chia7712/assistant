package codes.chia7712.assistant.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GenLexicalCommand {
  private static final String ISSUE = "18503";
  private static final String HOME = System.getProperty("user.home");
  private static final String PATH = HOME + "/Dropbox/hbase-jira/" + ISSUE + "/unittest";
  public static void main(String[] args) {
    Set<String> filenames = new TreeSet<>();
    for (char i = 65; i != 91; ++i) {
      filenames.add(format(i));
    }
    File folder = new File(PATH);
    if (folder.exists()) {
      for (File f : folder.listFiles()) {
        filenames.remove(f.getName());
        System.out.println("remove:" + f.getName());
      }
    }
    for (char i = 65; i != 91; ++i) {
      if (filenames.contains(format(i))) {
        System.out.println("mvn clean test -Dtest=Test" + i
                + "* -B -P skipSparkTests | tee ~/" + format(i));
      }
    }
  }
  private static String format(char i) {
    return "test_" + i + "_" + ISSUE;
  }
}
