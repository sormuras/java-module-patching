package org.astro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTests {

  @Test
  void accessPackagePrivateMember() {
    assertEquals("world", World.PACKAGE_PRIVATE_FIELD);
  }
}
