package utils.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SelectOption {

  public final String value;
  public final String prompt;
  public final boolean isPromptHtml;
  public final boolean isDividerAbove;

  public SelectOption(String value, String prompt, boolean isDividerAbove) {
    this(value, prompt, false, isDividerAbove);
  }

  /**
   * Use this constructor if the prompt contains HTML.
   *
   * @param isPromptHtml set to true if you can guarantee that the prompt contains safe HTML.
   */
  public SelectOption(String value, String prompt, boolean isPromptHtml, boolean isDividerAbove) {
    this.value = value;
    this.prompt = prompt;
    this.isPromptHtml = isPromptHtml;
	this.isDividerAbove = isDividerAbove;
  }

  public static LinkedHashMap<SelectOption, Boolean> fromEmpty(List<SelectOption> allOptions) {
    return allOptions.stream().collect(Collectors.toMap(e -> e, e -> false, (u, v) -> {
      throw new IllegalArgumentException();
    }, LinkedHashMap::new));
  }

  public static LinkedHashMap<SelectOption, Boolean> fromSelected(List<SelectOption> allOptions,
                                                                  List<String> selectedOptions) {
    return allOptions.stream().collect(Collectors.toMap(e -> e, e -> selectedOptions.contains(e.value), (u, v) -> {
      throw new IllegalArgumentException();
    }, LinkedHashMap::new));
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
