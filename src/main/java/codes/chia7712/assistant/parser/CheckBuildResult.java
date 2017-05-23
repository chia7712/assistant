package codes.chia7712.assistant.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CheckBuildResult {

  private static final String ISSUE = "acid";
  private static final String HOME = System.getProperty("user.home");
  private static final String PATH = HOME + "/Dropbox/hbase-jira/" + ISSUE + "/cat02";
  private static final File ROOT = new File(PATH);

  enum State {
    SUCCESS, FAILURE, NONE
  }

  public static void main(String[] arg) throws IOException {
    List<File> folders = new LinkedList<>();
    List<File> succeedFiles = new ArrayList<>(30);
    List<File> failedFiles = new ArrayList<>(30);
    List<File> others = new ArrayList<>(30);
    folders.add(ROOT);
    do {
      for (File f : folders.remove(0).listFiles()) {
        if (f.isFile()) {
          switch (checkState(f)) {
            case SUCCESS:
              succeedFiles.add(f);
              break;
            case FAILURE:
              failedFiles.add(f);
              break;
            default:
              others.add(f);
              break;
          }
        }
        if (f.isDirectory()) {
          folders.add(f);
        }

      }
    } while (!folders.isEmpty());
    System.out.println("------------------[SUCCESS]------------------");
    succeedFiles.forEach(v -> System.out.println(v.getAbsolutePath()));
    System.out.println("------------------[FAILURE]------------------");
    failedFiles.forEach(v -> System.out.println(v.getAbsolutePath()));
    System.out.println("------------------[OTHERS]------------------");
    others.forEach(v -> System.out.println(v.getAbsolutePath()));
  }

  private static State checkState(File f) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (isFailed(line)) {
          return State.FAILURE;
        }
        if (isSucceed(line)) {
          return State.SUCCESS;
        }
      }
    }
    return State.NONE;
  }

  private static boolean isFailed(String line) {
    return line.equals("[INFO] BUILD FAILURE");
  }

  private static boolean isSucceed(String line) {
    return line.equals("[INFO] BUILD SUCCESS");
  }
}
