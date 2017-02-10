# Journey

The `journey` package allows you to define and control a user's flow through a sequence of forms. It is intended to:

* Abstract a user's journey away from Controller classes into the application's configuration
* Make Controllers reusable across different journey stages, without requiring complicated logic in the Controller 
* Simplify back link handling
* Restore the user to a certain point in the journey after resuming a transaction

## Concepts

A Journey effectively allows you to define a flowchart style flow through your Controllers. You start off by defining
several **stages** which refer to Action methods (typically using a `Call`, i.e. a reverse router URI from your routes file).
Multiple stages can refer to the same Action - this is useful if you want to reuse a Controller several times within
the same Journey.

To move a user through a Journey, your Actions send **events** to the `JourneyManager`, which in turn results in a 
**transition**. The result of a transition is the next **stage** in the journey (e.g. invoking another Action to render 
a different form). This means that the Controller does not need to know anything about other screens in the journey - 
the information is all abstracted away.

A transition is defined in a `JourneyDefinition`, which must be registered on the `JourneyManager`. A transition may 
contain branching logic, typically based on an event's parameter value (if it has one).

You may also define **decision stages**. Rather than rendering a page, a decision stage invokes some arbitrary logic to 
determine the next stage the user should go to, typically driven by a selection they have made.

The JourneyManager maintains a history string which is ping-ponged between pages in a `ContextParam`. This means that 
in-page back links and the browser back button can be safely used together to navigate back through the journey, because 
each page knows exactly where it is in the Journey and what it means to "go back" from that point.

### Basic usage

In order to use the JourneyManager you must:

* Implement at least one `JourneyDefinitionBuilder` and define at least one journey within it
* `@Provides` a `Collection` of `JourneyDefinitionBuilders` from your application's Guice module
* `@Inject` a `JourneyManager` into all `Controller` classes which you are relevant to your journey
* Invoke the `startJourney()` and `performTransition()` methods from your controllers when you wish to start a journey
or perform a transition, respectively

## Defining a Journey

To define a Journey, subclass `JourneyDefinitionBuilder` and implement the `journeys()` method. You need to define 
`JourneyStages`, transitions, and one or more starting points.

``` java
public class ExampleJourneyDefinitionBuilder extends JourneyDefinitionBuilder {

  private final JourneyStage exportCategory = defineStage("exportCategory", controllers.categories.routes.ExportCategoryController.renderForm());
  private final JourneyStage goodsType = defineStage("goodsType", routes.GoodsTypeController.renderForm());
      
  @Override
  protected void journeys() {
  
    goodsCategoryStages();
  
    atStage(goodsType)
        .onEvent(Events.GOODS_TYPE_SELECTED)
        .branch()
        .when(GoodsType.PHYSICAL, moveTo(physicalGoodsSearch))
        .when(GoodsType.SOFTWARE, moveTo(notImplemented))
        .when(GoodsType.TECHNOLOGY, moveTo(notImplemented));
  
    physicalGoodsStages();
  
    defineJourney(JourneyDefinitionNames.EXPORT, exportCategory,  BackLink.to(routes.TradeTypeController.renderForm(),
        "Where are your items going?"));
  }
}  
```

### Defining stages

Use `defineStage()` to define a `JourneyStage`. This requires either a `Call` or a `Supplier<CompletionStage<Result>>`
which will be used to generate the form for that stage. It is highly recommended you provide a `Call` because this allows
the stage to be entered via a URI - so you can generate links which take the user directly there if you need. 

A `JourneyStage` also requires a unique internal name and an optional prompt for display on the back link.

``` java
JourneyStage categoryMedicinesDrugs = defineStage("categoryMedicinesDrugs", "Select drug type",
        controllers.categories.routes.MedicinesDrugsController.renderForm());
```

### Defining decision stages

Use `defineDecisionStage()` to define a `DecisionStage<T>`. A decision stage requires a `Decider` - this is effectively a 
`Supplier` which can asynchronously produce a single decision result object. `Decider`s will typically be injected into
the `JourneyDefinitionBuilder`.

Decision stages may also be defined with a converter function, which converts the result from the `Decider` into another
type. This is useful for writing a `Decider` which can be reused between different stages. In the example below, the 
`Decider` returns a `Collection`, but the decision can be a simple `Boolean` because of the converter.

``` java
DecisionStage<Boolean> decontrolsDecision = defineDecisionStage("hasDecontrols", controlCodeDecider, 
    r -> r.contains(ControlCodeDecider.ControlCodeDataType.DECONTROLS));
```

#### Example Decider

The following `Decider` bases its decision on the result from a web service call, in turn based on a value selected by a 
user. This would need to be injected into the JourneyManager. It is also assumed the DAO would be updated with the correct
value in a controller before any transition which uses this `Decider` is invoked.

``` java
public class DangerousCodeDecider implements Decider<Boolean> {

  @Inject
  AppDao dao;
  @Inject
  WebClient codeClient;
  
  @Override
  public CompletionStage<Boolean> decide() {
    codeClient.getCodeData(dao.getSelectedCode()).thenApply(e -> e.isDangerous());
  }
}
```

A `Decider` should never manipulate an application's state.

### Defining events

Events should be defined as `static final` fields in a separate class (canonically called `Events`). 

There are two types of Event - `JourneyEvent` and `ParamterisedJourneyEvent<T>`. The latter must always be invoked with
an argument of type `<T>`.

``` java
public class Events {

  public static final ParameterisedJourneyEvent<ExportCategory> EXPORT_CATEGORY_SELECTED =
      new ParameterisedJourneyEvent<>("EXPORT_CATEGORY_SELECTED", ExportCategory.class);

  public static final JourneyEvent EXPORT_CATEGORY_COULD_BE_DUAL_USE = new JourneyEvent("EXPORT_CATEGORY_COULD_BE_DUAL_USE");
  
}
```

The class `components.common.journey.StandardEvents` contains several pre-defined Events (such as next, confirm, etc) 
which should be used if applicable. 

### Defining stage transitions  

Use `atStage()` followed by `onEvent()` methods to start defining a transition. If you do not require any branching logic,
use `then()` to define the next stage in the journey. Otherwise, use `branch()` or `branchWith()`.

``` java
atStage(exportCategory)
    .onEvent(Events.EXPORT_CATEGORY_COULD_BE_DUAL_USE)
    .then(moveTo(categoryDualUse));
```

#### Event branching

Event branching can be performed using the argument of a `ParamterisedJourneyEvent<T>`. 
Here, the event `LIFE_TYPE_SELECTED` is a `ParamterisedJourneyEvent<LifeType>`:

``` java
atStage(categoryPlantsAnimals)
    .onEvent(Events.LIFE_TYPE_SELECTED)
    .branch()
    .when(LifeType.ENDANGERED, moveTo(categoryEndangeredAnimalStatic))
    .when(LifeType.NON_ENDANGERED, moveTo(categoryNonEndangeredAnimalStatic))
    .when(LifeType.PLANT, moveTo(categoryPlantStatic));
```

Enumerate your branch conditions by chaining `when()` method calls together. The event argument is compared to the first
argument of the `when()` method using `Object.equals()`.

It is recommended that you use an `Enum` as the parameter type for your `ParamterisedJourneyEvent`s, as this keeps the code
cleaner and means the `JourneyDefinitionBuilder` can assert that all branch cases are accounted for.

Use `.otherwise()` to provide a catch-all branch.

#### Converting event arguments

You can use `branchWith()` to convert an event argument to a different object type. In the example below, an enum argument
is converted to a `Boolean`, to reduce the number of branches which need to be defined.

``` java
atStage(categoryPlantsAnimals)
    .onEvent(Events.LIFE_TYPE_SELECTED)
    .branchWith(e -> e == LifeType.ENDANGERED_ANIMAL || e == LifeType.ENDANGERED_PLANT)
    .when(true, moveTo(endangeredSpecies))
    .when(false, moveTo(nonEndangered));
```

The lambda passed to `branchWith()` should be a pure function with no side-effects. 

### Defining decision transitions

Use `atDecisionStage()` to define the outcome of a decision stage, based on the result from a `Decider` (which may have
been converted by a converter function). This uses the same `when()`/`otherwise()` syntax as event branching. Values are
compared using `Object.equals()`.

``` java
atDecisionStage(decontrolsDecision)
    .decide()
    .when(true, moveTo(decontrols))
    .when(false, moveTo(technicalNotesDecision));
```

### Defining starting points

You must define at least one starting stage using the `defineJourney()` method. If your application requires multiple 
starting points (for example edit journeys which skip certain parts of the flow), you can invoke `defineJourney()` several
times, with different starting stages. Each starting point must have a unique name, which must be referenced when 
`startJourney()` is invoked.

A starting stage can be decision, if required.

## The JourneyManager

The `JourneyManager` should be used by form submit actions, typically after any validation and persistence code has run.
The `performTransition` methods return a `CompletionStage<Result>` which can be sent out to Play for it to perform its 
response handling.

The controller can do simple logic to determine which event to raise, or which argument to give a parameterised event. 
More complex logic which requires additional lookups etc should be done in a `Decider`, to keep controller code simple.

### Example Controller use

The following example demonstrates the `JourneyManager` being used to determine the next controller in the journey after
the incoming form has been successfully validated.

``` java 
public CompletionStage<Result> handleSubmit() {
  Form<DualUseForm> form = formFactory.form(DualUseForm.class).bindFromRequest();
  if (form.hasErrors()) {
    return completedFuture(ok(dualUse.render(form)));
  }

  boolean isDualUse = form.get().isDualUse;
  permissionsFinderDao.saveIsDualUseGood(isDualUse);

  //Parameterised event which takes a boolean
  return journeyManager.performTransition(Events.IS_DUAL_USE, isDualUse);
}
```

### Generating transition links

In some cases the `JourneyManager` can be asked to produce the URI for a transition. This can be used as the `href` of a
link, removing the need to add a form submit handler just for simple navigation. To generate a URI, call `JourneyManager.uriForTransition()`
in a controller, and pass the result to your template for use as an `href` attribute.

Note that if the transition involves a decision stage, this method cannot be used.

## Journey serialization

Your Guice module should provide a `JourneySerialiser`, which the `JourneyManager` will serialise the user's current journey
history to whenever it is modified. To restore a previously saved journey from the `JourneySerialiser`, use 
`JourneyManager.restoreCurrentStage()`.

## Back links and back handling

By default, the "back" link at the top of every page takes the user back one stage in a journey (ignoring decision stages).
In some controllers you may wish to override this behaviour. 

* If a back link prompt is not specified for a stage, the default of "Back" is used.
* To set a different back link within a controller, call `ViewUtil.overrideBackLink()` before rendering the template.
* To hide the back link, call `JourneyManager.hideBackLink()` before rendering the template.

By default, the first stage of a Journey has no back link. You can pass a `BackLink` to the `defineJourney()` method to
override this behaviour.

Create a `BackLink` using the `BackLink.to()` method. A back link must refer to a URL `Call` (i.e. from the reverse router).

### Traversing backwards through the journey

In some cases, a transition may navigate a user "back" through a journey (e.g. "back to search results"). This should
effectively remove ("pop") stages from the user's journey history. To do this, use `backTo()` instead of `moveTo()` when
defining a transition.

When a `backTo()` transition is invoked, the journey manager traverses backwards through the user's journey history to 
find the latest instance of the target stage (ignoring the current stage). If the stage is not found, the entire history
is removed and the target stage becomes to first stage of the journey.

## Generating a flowchart

The `GraphvizSerialiser` test class can be used to produce a [Graphviz](http://graphviz.org/) representation of the 
journeys defined in a `JourneyDefinitionBuilder`. The output can then be passed to [Webgraphviz](http://www.webgraphviz.com/) 
to produce a visual flowchart, which is useful for debugging.