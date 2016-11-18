package components.common.journey;

/**
 * Interface for saving and restoring a serialised Journey object in persistent storage.
 * Typically implemented by a subclass of CommonRedisDao.
 */
public interface JourneySerialiser {

  /**
   * Returns the serialised Journey string from persistent storage
   * @return the serialised journey
   */
  String readJourneyString();

  /**
   * Writes the serialised Journey string to persistent storage
   * @param journeyString The journey string to write
   */
  void writeJourneyString(String journeyString);

}
