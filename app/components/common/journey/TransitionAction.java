package components.common.journey;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class TransitionAction {

  final static class MoveStage extends TransitionAction {

    private final CommonStage destinationStage;

    public MoveStage(CommonStage destinationStage) {
      this.destinationStage = destinationStage;
    }

    public CommonStage getDestinationStage() {
      return destinationStage;
    }
  }

  final static class Branch extends TransitionAction {

    final Supplier<?> transitionArgumentSupplier;
    final Function<Object, ?> eventArgumentConverter;

    final Map<String, TransitionAction> resultMap;
    final TransitionAction elseTransition;

    Branch(Supplier<?> transitionArgumentSupplier, Function<? super Object, ?> eventArgumentConverter, Map<String,
        TransitionAction> resultMap, TransitionAction elseTransition) {
      this.transitionArgumentSupplier = transitionArgumentSupplier;
      this.eventArgumentConverter = eventArgumentConverter;
      this.resultMap = resultMap;
      this.elseTransition = elseTransition;
    }
  }



}
