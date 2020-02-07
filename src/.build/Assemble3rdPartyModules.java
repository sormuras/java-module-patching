import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class Assemble3rdPartyModules {

  public static void main(String[] args) throws Exception {
    System.out.println("Assemble 3rd-party modules");

    var lib = Files.createDirectories(Path.of("lib"));
    load(lib, "org.apiguardian", "apiguardian-api", "1.1.0");
    load(lib, "org.opentest4j", "opentest4j", "1.2.0");
    var platform = "1.6.0";
    load(lib, "org.junit.platform", "junit-platform-commons", platform);
    load(lib, "org.junit.platform", "junit-platform-console", platform);
    load(lib, "org.junit.platform", "junit-platform-engine", platform);
    load(lib, "org.junit.platform", "junit-platform-launcher", platform);
    load(lib, "org.junit.platform", "junit-platform-reporting", platform);
    var jupiter = "5.6.0";
    load(lib, "org.junit.jupiter", "junit-jupiter", jupiter);
    load(lib, "org.junit.jupiter", "junit-jupiter-api", jupiter);
    load(lib, "org.junit.jupiter", "junit-jupiter-engine", jupiter);
    load(lib, "org.junit.jupiter", "junit-jupiter-params", jupiter);
  }

  static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  static void load(Path lib, String group, String artifact, String version) throws Exception {
    var repository = "https://repo.maven.apache.org/maven2";
    var file = artifact + '-' + version + ".jar";
    var source = String.join("/", repository, group.replace('.', '/'), artifact, version, file);
    var target = lib.resolve(file);
    if (Files.exists(target)) return;
    System.out.println("< " + source);
    var request = HttpRequest.newBuilder(URI.create(source)).GET().build();
    var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(target));
    if (response.statusCode() == 200) return;
    throw new Error("Non-200 status code: " + response);
  }
}
