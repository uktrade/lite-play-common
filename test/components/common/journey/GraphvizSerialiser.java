package components.common.journey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test class for generating Graphviz digraph syntax from a JourneyDefinition.
 */
public class GraphvizSerialiser {

  public StringBuilder generateGraphvizSyntax(JourneyDefinition journeyDefinition) {

    StringBuilder b = new StringBuilder("digraph journey {\n" +
        "  graph [ rankdir = \"LR\"];\n" +
        "  node [shape = rectangle, fontsize=10];\n\n");

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

      b.append("  ").append(transition.getStartStage().getInternalName());
      b.append(" -> ").append(transition.getEndStage().getInternalName());
      b.append(" [ label = \"").append(edgeLabel).append("\", fontsize = 10").append(" ];\n");

      if (transition.getEndStage() instanceof DecisionStage) {
        decisionStages.add(transition.getEndStage());
      }
    }

    decisionStages.forEach(e -> b.append(e.getInternalName() + " [shape = diamond];\n"));

    b.append("}");

    return b;
  }

}
