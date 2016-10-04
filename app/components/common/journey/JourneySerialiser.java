package components.common.journey;

/**
 * Interface for storing and retrieving a Journey object in persistent storage.
 * Typically implemented by a subclass of CommonRedisDao.
 */
public interface JourneySerialiser {

  /**
   * Returns the Journey from persistent storage
   * @return the Journey
   */
  Journey readJourney();

  /**
   * Writes the serialised Journey to persistent storage
   * @param journey The journey to write
   */
  void writeJourney(Journey journey);

}
