## GOV.UK Design System

Assets in lite-play-common are built from:

 * [govuk_frontend](https://github.com/alphagov/govuk_frontend) - v2.5

### Updating GOV.UK Assets

Delete all files in ''app/assets/stylesheets" except the "lite" folder

Copy "govuk-frontend/src/" into "lite-play-common/app/assets/stylesheets"

Then delete all .js, .njk and .yaml files in that folder

Move folders from "assets" folder into "lite-play-common/public"

  * import lite scss at bottom of file: `@import "lite/lite";`

change `$govuk-assets-path:` to `$govuk-assets-path: "/common/assets/" !default;`
