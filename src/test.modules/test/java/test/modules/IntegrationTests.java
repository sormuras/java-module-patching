package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IntegrationTests {

  @Test
  void accessWorld() {
    assertEquals("world", org.astro.World.name());
  }

  @Test
  void accessGreetings() throws ClassNotFoundException {
    // assertEquals("Main", com.greetings.Main.class.getSimpleName()); // Does not even compile!

    var main = Class.forName("com.greetings.Main"); // Loading a class from a non-exported package.

    var exception =
        assertThrows(
            IllegalAccessException.class,
            () -> main.getMethod("main", String[].class).invoke(null, (Object) null));

    assertEquals(
        "class test.modules.IntegrationTests (in module test.modules)"
            + " cannot access class com.greetings.Main (in module com.greetings)"
            + " because module com.greetings"
            + " does not export com.greetings"
            + " to module test.modules",
        exception.getMessage());
  }
}
