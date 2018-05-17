package components.common.journey;

public class JourneyException extends RuntimeException {

  private final CommonStage stage;
  private final CommonJourneyEvent event;


  public JourneyException(String message) {
    super(message);
    this.stage = null;
    this.event = null;

  }

  public JourneyException(String message, CommonStage stage, CommonJourneyEvent event) {
    super(message);
    this.stage = stage;
    this.event = event;
  }

  public JourneyException(String message, CommonStage stage, CommonJourneyEvent event, Throwable cause) {
    super(message, cause);
    this.stage = stage;
    this.event = event;
  }

  @Override
  public String getMessage() {
    String detailMessage = super.getMessage();
    if (stage == null && event == null) {
      return detailMessage;
    } else {
      String stageMessage = stage != null ? stage.toString() : "unknown stage";
      String eventMessage = event != null ? event.toString() : "unknown event";
      return detailMessage + " [" + stageMessage + " " + eventMessage + "]";
    }
  }
}

