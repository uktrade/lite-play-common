package components.common.journey;

import java.util.Map;
import java.util.function.Function;

final class BranchAction implements TransitionAction {

  final Function<Object, ?> eventArgumentConverter;

  final Map<Object, TransitionAction> resultMap;
  final TransitionAction elseTransition;

  BranchAction(Function<? super Object, ?> eventArgumentConverter, Map<Object, TransitionAction> resultMap,
               TransitionAction elseTransition) {
    this.eventArgumentConverter = eventArgumentConverter;
    this.resultMap = resultMap;
    this.elseTransition = elseTransition;
  }
}
