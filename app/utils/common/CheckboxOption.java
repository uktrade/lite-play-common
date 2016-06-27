package utils.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CheckboxOption {

  public final String value;
  public final String prompt;

  public CheckboxOption(String value, String prompt) {
    this.value = value;
    this.prompt = prompt;
  }

  public static LinkedHashMap<CheckboxOption, Boolean> fromEmpty(List<CheckboxOption> allOptions) {
    LinkedHashMap<CheckboxOption, Boolean> collect = allOptions.stream().collect(Collectors.toMap(e -> e, e -> false, (u, v) -> {
      throw new IllegalArgumentException();
    }, LinkedHashMap::new));

    return collect;
  }


  public static LinkedHashMap<CheckboxOption, Boolean> fromSelected(List<CheckboxOption> allOptions, List<String> selectedOptions) {
    LinkedHashMap<CheckboxOption, Boolean> collect = allOptions.stream().collect(Collectors.toMap(e -> e, e -> selectedOptions.contains(e.value), (u, v) -> {
      throw new IllegalArgumentException();
    }, LinkedHashMap::new));

    return collect;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return value.equals(obj);
  }

  @Override
  public String toString() {
    return "CheckboxOption[" + value + "]";
  }
}
