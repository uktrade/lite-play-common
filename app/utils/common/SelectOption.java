package utils.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SelectOption {

  public final String value;
  public final String prompt;

  public SelectOption(String value, String prompt) {
    this.value = value;
    this.prompt = prompt;
  }

  public static LinkedHashMap<SelectOption, Boolean> fromEmpty(List<SelectOption> allOptions) {
    LinkedHashMap<SelectOption, Boolean> collect = allOptions.stream().collect(Collectors.toMap(e -> e, e -> false, (u, v) -> {
      throw new IllegalArgumentException();
    }, LinkedHashMap::new));

    return collect;
  }


  public static LinkedHashMap<SelectOption, Boolean> fromSelected(List<SelectOption> allOptions, List<String> selectedOptions) {
    LinkedHashMap<SelectOption, Boolean> collect = allOptions.stream().collect(Collectors.toMap(e -> e, e -> selectedOptions.contains(e.value), (u, v) -> {
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
    return "SelectOption[" + value + "]";
  }
}
