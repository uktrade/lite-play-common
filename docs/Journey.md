# JourneyManager

The `JourneyManager` allows you to define a user's journey through a series of forms. It is intended to:

* Abstract a user's journey away from Controller classes into the application's configuration
* Make Controllers reusable for different journey stages, without requiring complicated logic in the Controller 
* Simplify back link handling
* Restore the user to a certain point in the journey after resuming a transaction

## Concepts

A Journey effectively allows you to define a flowchart style flow through your Controllers. You start off by defining
several **stages** which refer to Action methods (typically using a `Call`, i.e. a reverse router URI from your routes file).
Multiple stages can refer to the same Action - this is useful if you want to reuse a Controller several times within
the same Journey.

To move a user through a Journey, your Actions send **events** to the `JourneyManager`, which in turn results in a 
**transition**. The result of a transition is the next **stage** in the journey (i.e. invoking another Action to render 
a different form). This means that the Controller does not need to know anything about other screens in the journey - 
the information is all abstracted away.

A transition is defined in a `JourneyDefinition`, which must be registered on the `JourneyManager`. A transition may 
contain branching logic, typically based on an event's parameter value (if it has one).

The JourneyManager maintains a history string which is ping-ponged between pages in a `ContextParam`. This means that 
in-page back links and the browser back button can be safely used together to navigate back through the journey, because 
each page knows exactly where it is in the Journey and what it means to "go back" from that point.

### Basic usage

In order to use the JourneyManager you must:

* Implement at least one `JourneyDefinitionBuilder` and define at least one journey within it
* `@Provides` a `Collection` of `JourneyDefinitionBuilders` from your application's Guice module
* `@Inject` a `JourneyManager` into all `Controller` classes which you are relevant to your journey
* Invoke the `startJourney()` and `performTransition()` methods from your controllers when you wish to start a journey
or peform a transition, respectively

## Defining a Journey

To define a Journey, subclass `JourneyDefinitionBuilder` and implement the `journeys()` method. You need to define 
`JourneyStages`, transitions, and one or more starting points.

``` java
public class ExampleJourneyDefinitionBuilder extends JourneyDefinitionBuilder {

  private final JourneyStage exportCategory = defineStage("exportCategory", "What are you exporting?",
      controllers.categories.routes.ExportCategoryController.renderForm());
  private final JourneyStage goodsType = defineStage("goodsType", "Are you exporting goods, software or technical information?",
      routes.GoodsTypeController.renderForm());
      
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

A `JourneyStage` also requires a unique internal name and an external name for display on the back link.

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

The class `components.common.journey.StandardEvents` contains several pre-defined Events which you may find useful.

### Defining transitions  

Use `atStage()` followed by `onEvent()` methods to start defining a transition. If you do not require any branching logic,
use `then()` to define the next stage in the journey. Otherwise, use `branch()` or `branchWith()`.

``` java
atStage(exportCategory)
    .onEvent(Events.EXPORT_CATEGORY_COULD_BE_DUAL_USE)
    .then(moveTo(categoryDualUse));
```

#### Simple branching

Simple Branching can be performed using the argument of a `ParamterisedJourneyEvent<T>`. 
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
argument of the `when()` method using `Object.equals()` - the first matching condition is invoked.

It is recommended that you use an `Enum` as the parameter type for your `ParamterisedJourneyEvent`s, as this keeps the code
cleaner and means the `JourneyDefinitionBuilder` can assert that all branch cases are accounted for.

#### Supplier branching

For all event types, you can use `branchWith()` to branch on an arbitrary value.
