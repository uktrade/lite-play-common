package components.common.journey;

public class ParameterisedJourneyEvent<T> implements CommonJourneyEvent {

  private final Class<T> paramType;
  private final String eventMnem;

  public ParameterisedJourneyEvent(String eventMnem, Class<T> paramType) {
    //super(eventMnem);
    this.eventMnem = eventMnem;
    this.paramType = paramType;
  }

  public Class<T> getParamType() {
    return paramType;
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
