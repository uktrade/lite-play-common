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
