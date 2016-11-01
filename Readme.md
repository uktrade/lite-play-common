# LITE Play Common

Common code for use in all LITE Play projects.


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