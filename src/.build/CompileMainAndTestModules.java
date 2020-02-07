import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

public class CompileMainAndTestModules {

  public static void main(String[] args) throws Exception {
    System.out.println("Compile main and test modules");

    // compile modules of main realm
    run(
        "javac",
        "-d",
        "out/classes/main",
        "--module-source-path",
        "src/*/main/java",
        "--module",
        "org.astro,com.greetings");

    jar("main", "org.astro");
    jar("main", "com.greetings");

    // compile modules of test realm
    run(
        "javac",
        "-d",
        "out/classes/test",
        "--module-source-path",
        "src/*/test/java",
        "--module-path",
        "out/modules/main" + File.pathSeparator + "lib",
        "--patch-module",
        "org.astro=out/modules/main/org.astro.jar",
        "--module",
        "org.astro,test.modules");

    jar("test", "org.astro");
    jar("test", "test.modules");
  }

  static void run(String tool, String... args) {
    var line = tool + " " + String.join(" ", args);
    System.out.println("- " + line);
    var code = ToolProvider.findFirst(tool).orElseThrow().run(System.out, System.err, args);
    if (code != 0) throw new AssertionError("Non-zero exit code: " + line);
  }

  static void jar(String realm, String module) throws Exception {
    Files.createDirectories(Path.of("out", "modules", realm));
    run(
        "jar",
        "--create",
        "--file",
        "out/modules/" + realm + "/" + module + ".jar",
        "-C",
        "out/classes/" + realm + "/" + module,
        ".");
  }
}
