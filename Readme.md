# LITE Play Common

Common code for use in all LITE Play projects.

## Journey Manager

The `JourneyManager` and associated classes allow you to define a user's journey through the application. 
[Read further details here](docs/Journey.md).

## Context Params

Context params are used to maintain session data, as an alternative to cookies. They work by "ping-ponging" data 
between pages as GET or POST parameters attached to every request. This helps avoid common issues encountered when using
cookies, such as the browser back button not restoring a value to its previous state, or multiple tabs overwriting each
other's data.

To use context params, subclass `ContextParamProvider` and implement the required name method, then `@Provide` a 
`ContextParamManager` composed of all your `ContextParamProvider`s from your Guice module. 

For context params to work correctly, every `<form>` in your application must include the `@contextFormParams` template.
Additionally, every generated URL must be decorated by passing it to `ContextParamManager.addParamsToCall`. 

The Journey Manager uses context params to pass the current journey state between forms, so you must do this if you want
to use the Journey Manager.

## Error Pages

The ErrorHandler class in /app should be picked up automatically by Play to override the default error handling and
instead output Gov UK styled error pages.

If you want to have the Play default error handling behaviour you can add the following line to your application config:

`play.http.errorHandler = "play.http.DefaultHttpErrorHandler"`

When the application is running in development mode stack traces and extra error information should be shown on the page.
In production mode the extra information will not be shown by default. If you want to see that extra information in
production you can add the following line to your application config:

`errorDetailEnabled = true`

## Client side validation

Any forms sent out which contain elements with `data-validation` attributes will be validated client side.

`data-validation` attributes should be set on form fields and form-groups and contain field validation information in 
escaped JSON format which can be obtained with a call to `ViewUtil.fieldValidationJSON(field)`, where field is a 
`Form.field`.

### Skipping validation

You might have a form with multiple submit buttons and only want client side validation to run for some of them. For the
submit buttons that you want to skip validation you can put an attribute of `data-skip-validation` on them.

### Validating sub-groups of fields in a form

By default when a form is submitted all of the elements in the form with `data-validation` attributes will be validated.
In cases where you might have multiple submit actions in a form, and you want to scope the submit-validation to a 
sub-group of elements you can put a `data-validation-group` attribute on the submit action and the elements you wish it
to validate, using a distinct attribute value for the group, e.g. `data-validation-group="postcode"`

### Validating from custom JS

If you have some custom javascript and want to validate a form you can call:

```javascript
var validationResult = LITECommon.ClientSideValidation.validateForm($('#form'), $('#triggeringelement'));
```

Passing it a jQuery-wrapped form element that contains the fields to validate as well as a jQuery-wrapped element that 
triggered the validation (used to find the triggers validation-group).

### Custom JS validation
You might find that you need to use a custom validation function instead of the standard client side validation. In order to
do so, you can call setValidationFunction like so:
 
```javascript
LITECommon.ClientSideValidation.setValidationFunction(function() {

  var validationFailures = []; // this array is expected to store objects of the following structure - {field: $('#xyz'), message: 'message'}.  
  
  // custom validation code
  ...
  return validationFailures;
});  
```

## JWT Request Filter
This request filter (`JwtRequestFilter`) adds an `Authorization` header containing a signed JWT to a `WSRequest`. The JWT 
is comprised of a a subset of fields from an `AuthInfo` object, id, email, and full name. All signed by secret key shared 
between both parties of the request. 

`JwtRequestFilter` should be injected wherever an instance is required, this relies on both `JwtRequestFilterConfig` and 
`SpireAuthManager` to have providers defined (or otherwise be injectable).

Example `JwtRequestFilterConfig` provider, note: `jwtSharedSecret` must be at least 64 bytes in length.
```java
@Provides
public JwtRequestFilterConfig provideJwtRequestFilterConfig(@Named("jwtSharedSecret") String jwtSharedSecret) {
  return new JwtRequestFilterConfig(jwtSharedSecret, "lite-sample-service");
}
```

Example usage with `WSClient`:
```java
public void doGet() {
  JwtRequestFilter filter; // Injected
  wsClient.url("/example").setRequestFilter(filter);
}
```

Sample `Authorization` header:
```
Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzb21lLXNlcnZpY2UiLCJleHAiOjE1MDgyNDkyNjQsImp0aSI6IkRiUnNVOVlRdzYz
NUZGTENaTVJyS3ciLCJpYXQiOjE1MDgyNDg2NjQsIm5iZiI6MTUwODI0ODU0NCwic3ViIjoiMTIzNDU2IiwiZW1haWwiOiJleGFtcGxlQGV4YW1wbGUub3Jn
IiwiZnVsbE5hbWUiOiJNciB0ZXN0In0.Lkpv5sJGAVn3W3rOposUVf1Ei6UvPRwdR2cuykuiAnE
```

Which produces the following JWT (signature omitted):
```
{
  "typ": "JWT",
  "alg": "HS256"
}.
{
  "iss": "some-service",
  "exp": 1508249264,
  "jti": "DbRsU9YQw635FFLCZMRrKw",
  "iat": 1508248664,
  "nbf": 1508248544,
  "sub": "123456",
  "email": "example@example.org",
  "fullName": "Mr test"
}
```
