package components.common.journey;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test class for generating Graphviz digraph syntax from a JourneyDefinition.
 */
public class GraphvizSerialiser {

  public StringBuilder generateGraphvizSyntax(Collection<JourneyDefinition> journeyDefinitions) {

    StringBuilder b = new StringBuilder("digraph journey {\n" +
        "  graph [ rankdir = \"LR\"];\n" +
        "  node [shape = rectangle, fontsize=10];\n\n");

    Set<String> syntaxLines = new HashSet<>();

    //Collect graphviz syntax lines from all journeys into a unique set to reduce duplication
    for (JourneyDefinition journeyDefinition : journeyDefinitions) {
      List<GraphViewTransition> graphViewTransitions = journeyDefinition.asGraphViewTransitions();

      Set<CommonStage> decisionStages = new HashSet<>();

      for (GraphViewTransition transition : graphViewTransitions) {
        String edgeLabel;
        if (transition.isConditional()) {
          //For boolean transitions, we have to show the transition name as well as the value
          //(just "true" or "false" doesn't make any sense as an edge label)
          String conditionValue = transition.getConditionValue();
          if ("true".equals(conditionValue) || "false".equals(conditionValue)) {
            edgeLabel = transition.getEventName() + " = " + conditionValue;
          } else {
            //In most cases, the value of the condition provides sufficient information without cluttering the graph
            edgeLabel = conditionValue;
          }
        } else {
          //If there's no condition, use the event name
          edgeLabel = transition.getEventName();
        }

        String transitionString = String.format("  %s -> %s [ label =\"%s\", fontsize=10 ];\n",
            transition.getStartStage().getInternalName(), transition.getEndStage().getInternalName(), edgeLabel);

        syntaxLines.add(transitionString);

        //Record decision stages so they can be marked as diamonds
        if (transition.getEndStage() instanceof DecisionStage) {
          decisionStages.add(transition.getEndStage());
        }
      }

      //Special case in case the first stage of a journey needs marking as a decision
      if (journeyDefinition.getStartStage() instanceof DecisionStage) {
        decisionStages.add(journeyDefinition.getStartStage());
      }

      //Mark decision stages as diamonds
      syntaxLines.addAll(decisionStages.stream()
          .map(e -> String.format("  %s [shape = diamond];\n", e.getInternalName()))
          .collect(Collectors.toSet()));

      //Serialise the "start" transition
      b.append(String.format("  %s -> %s;\n", journeyDefinition.getJourneyName(), journeyDefinition.getStartStage().getInternalName()));
      b.append(String.format("  %s [shape = ellipse]", journeyDefinition.getJourneyName()));
    }

    //Print all de-duplicated syntax lines
    syntaxLines.forEach(b::append);

    b.append("}");

    return b;
  }

}
