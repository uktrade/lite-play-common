package components.common.journey;

public class StandardEvents {

  public static final JourneyEvent NEXT = new JourneyEvent("_NEXT");

  public static final JourneyEvent NONE_OF_THE_ABOVE = new JourneyEvent("_NOTA");

  public static final JourneyEvent YES = new JourneyEvent("_YES");

  public static final JourneyEvent NO = new JourneyEvent("_NO");

  public static final JourneyEvent CANCEL = new JourneyEvent("_CANCEL");

}
