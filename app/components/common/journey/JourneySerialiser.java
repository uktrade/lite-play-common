package components.common.journey;

/**
 * Interface for saving and restoring a serialised Journey object in persistent storage.
 * Typically implemented by a subclass of CommonRedisDao.
 */
public interface JourneySerialiser {

  /**
   * Returns the serialised Journey string from persistent storage
   * @param journeyName The name of journey to read
   * @return the serialised journey
   */
  String readJourneyString(String journeyName);

  /**
   * Writes the serialised Journey string to persistent storage
   * @param journeyName The name of journey to write
   * @param journeyString The journey string to write
   */
  void writeJourneyString(String journeyName, String journeyString);

}
