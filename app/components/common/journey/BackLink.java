package components.common.journey;

import play.mvc.Call;

public class BackLink {

  private final Call call;
  private final String prompt;

  private BackLink(Call call, String prompt) {
    this.call = call;
    this.prompt = prompt;
  }

  public static BackLink to(Call call, String prompt) {
    return new BackLink(call, prompt);
  }

  public Call getCall() {
    return call;
  }

  public String getPrompt() {
    return prompt;
  }
}
