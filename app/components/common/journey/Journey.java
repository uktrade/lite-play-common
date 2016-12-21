package components.common.journey;

import org.apache.commons.lang3.StringUtils;
import play.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Journey {

  private final String journeyName;
  private final Deque<String> historyQueue;

  private Journey(String journeyName, Deque<String> historyQueue) {
    this.journeyName = journeyName;
    this.historyQueue = historyQueue;
  }

  public String getCurrentStageHash() {
    return historyQueue.peekLast();
  }

  public String serialiseToString() {
    return journeyName + JourneyManager.JOURNEY_NAME_SEPARATOR_CHAR +  String.join(JourneyManager.JOURNEY_STAGE_SEPARATOR_CHAR, historyQueue);
  }

  public void pushStage(String stageHash) {
    historyQueue.addLast(stageHash);
  }

  public void popBackToStage(String targetStageHash) {
    //Always discard the current stage (in case it's also the target stage)
    historyQueue.pollLast();

    //Remove stages from the journey until the target is found (note this may deplete the entire journey)
    String poppedStageHash;
    do {
      poppedStageHash = historyQueue.pollLast();
    } while (poppedStageHash != null && !targetStageHash.equals(poppedStageHash));

    if (historyQueue.isEmpty()) {
      Logger.warn("Journey pop back to {} depleted the history queue", targetStageHash);
    }

    //The target stage should now the latest stage in the journey
    historyQueue.addLast(targetStageHash);
  }

  public String getJourneyName() {
    return journeyName;
  }

  public Deque<String> getHistoryQueue() {
    return historyQueue;
  }

  public static Journey fromString(String journeyString) {

    if (StringUtils.isBlank(journeyString)) {
      return null;
    }

    if (!journeyString.contains(JourneyManager.JOURNEY_NAME_SEPARATOR_CHAR)) {
      throw new JourneyManagerException("Invalid journey string, missing ~ character");
    } else {
      String[] journeyStringSplit = journeyString.split(JourneyManager.JOURNEY_NAME_SEPARATOR_CHAR, 2);
      String journeyName = journeyStringSplit[0];
      String journeyHistory = journeyStringSplit[1];

      List<String> history = new ArrayList<>();
      Collections.addAll(history, journeyHistory.split(JourneyManager.JOURNEY_STAGE_SEPARATOR_CHAR));

      return new Journey(journeyName, new ArrayDeque<>(history));
    }
  }

  public static Journey createJourney(String journeyName, String initialStateHash) {
    return new Journey(journeyName, new ArrayDeque<>(Collections.singletonList(initialStateHash)));
  }
}
