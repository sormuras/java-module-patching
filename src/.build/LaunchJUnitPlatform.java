import java.io.File;

public class LaunchJUnitPlatform {

  public static void main(String[] args) throws Exception {
    System.out.println("Launch JUnit Platform");

    start(
        ProcessHandle.current().info().command().orElse("java"),
        "--module-path",
        "out/modules/test" + File.pathSeparator + "out/modules/main" + File.pathSeparator + "lib",
        "--add-modules",
        "test.modules",
        "--patch-module",
        "org.astro=out/modules/main/org.astro.jar",
        "--module",
        "org.junit.platform.console",
        "--disable-banner",
        "--reports-dir=" + "out/test-reports/" + "test.modules",
        "--select-module",
        "test.modules");

    start(
        ProcessHandle.current().info().command().orElse("java"),
        "--module-path",
        "out/modules/test" + File.pathSeparator + "out/modules/main" + File.pathSeparator + "lib",
        "--add-modules",
        "test.modules",
        "--patch-module",
        "org.astro=out/modules/main/org.astro.jar",
        "--module",
        "org.junit.platform.console",
        "--disable-banner",
        "--reports-dir=" + "out/test-reports/" + "org.astro",
        "--select-module",
        "org.astro");
  }

  static void start(String... command) throws Exception {
    var line = String.join(" ", command);
    System.out.println("+ " + line);
    var process = new ProcessBuilder(command).inheritIO().start();
    if (process.waitFor() != 0) throw new AssertionError("Non-zero exit code: " + line);
  }
}
