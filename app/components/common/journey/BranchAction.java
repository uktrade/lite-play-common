package components.common.journey;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

final class BranchAction implements TransitionAction {

  final Supplier<?> transitionArgumentSupplier;
  final Function<Object, ?> eventArgumentConverter;

  final Map<String, TransitionAction> resultMap;
  final TransitionAction elseTransition;

  BranchAction(Supplier<?> transitionArgumentSupplier, Function<? super Object, ?> eventArgumentConverter, Map<String,
      TransitionAction> resultMap, TransitionAction elseTransition) {
    this.transitionArgumentSupplier = transitionArgumentSupplier;
    this.eventArgumentConverter = eventArgumentConverter;
    this.resultMap = resultMap;
    this.elseTransition = elseTransition;
  }
}
