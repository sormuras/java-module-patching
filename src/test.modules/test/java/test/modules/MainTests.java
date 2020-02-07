package test.modules;

public class MainTests {
  public static void main(String[] args){
    assert "test.modules".equals(MainTests.class.getModule().getName());
  }
}
