package components.common.journey;

public class JourneyEvent implements CommonJourneyEvent {

  private final String eventMnem;

  public JourneyEvent(String eventMnem) {
    this.eventMnem = eventMnem;
  }

  @Override
  public String getEventMnemonic() {
    return eventMnem;
  }

  @Override
  public String toString() {
    return "event '" + eventMnem +  "'";
  }
}
