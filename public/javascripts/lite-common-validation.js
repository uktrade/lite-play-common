/*jshint laxcomma: true, laxbreak: true, strict: false, devel: true */
/*global $:false, jQuery:false */

var LITECommon = LITECommon || {};
LITECommon.ClientSideValidation = {
  clientSideDataAttrName: 'data-clientside-validation',
  // These selectors exclude error targets and message parents inside panels, such as radio booleans with content.
  // This stops the error styling and message being applied to any fields inside these panels.
  // Fields inside panels are never validated client-side as dependent validation isn't implemented.
  formGroupErrorTargetSelector: '.clientside-form-group-error-target:not(.govuk-form-group .panel .govuk-form-group .clientside-form-group-error-target):not(.govuk-form-group .panel .govuk-form-group.clientside-form-group-error-target)',
  errorMessageParentSelector: '.clientside-error-message-parent:not(.govuk-form-group .panel .form-group .clientside-error-message-parent):not(.govuk-form-group .panel .form-group.clientside-error-message-parent)',
  _validationFunction: null,
  _$triggeringElement: null,

  /**
   * Handle validation via a submit event by parsing the event and find the triggering element and then calling the
   * validateForm() function.
   *
   * @param event form submit event
   * @returns {boolean} True if all fields in the form passed validation
   */
  handleSubmit: function(event) {
    "use strict";
    var $form = $(event.target);

    //Default to the first submit element in the form if none clicked
    var $triggeringElement = LITECommon.ClientSideValidation._$triggeringElement || $form.find('button[type=submit]').get(0);
    if ($triggeringElement.length && $form.has($triggeringElement) && $triggeringElement.is('[data-skip-validation]')) {
      // Skip validation if the active element (typically the element that cause the form submit) contains a data-skip-validation attribute
      return true;
    }

    return LITECommon.ClientSideValidation.validateForm($form, $triggeringElement);
  },

  onSubmitClick: function(event) {
    LITECommon.ClientSideValidation._$triggeringElement = $(event.target);
  },

  /**
   * Validate all fields in a form which have data-validation attributes on them describing how the field should be
   * validated.
   *
   * @param $form The form element with fields to validate
   * @param $triggeringElement The element that caused the validation to happen
   * @returns {boolean} True if all fields in the form passed validation
   */
  validateForm: function ($form, $triggeringElement) {
    "use strict";

    var validationFailures;
    if (LITECommon.ClientSideValidation._validationFunction != null) { // perform custom validation

      // clear errors
      var validatableElements = LITECommon.ClientSideValidation._findValidatableElements($form, $triggeringElement);
      validatableElements.each(function (i, field) {
        LITECommon.ClientSideValidation._clearFieldClientSideError(field);
      });

      validationFailures = LITECommon.ClientSideValidation._validationFunction();
    } else {
      validationFailures = LITECommon.ClientSideValidation.standardValidation($form, $triggeringElement)
    }

    LITECommon.ClientSideValidation._addErrorToPageTitle(validationFailures);
    LITECommon.ClientSideValidation._addErrorSummary(validationFailures);
    return validationFailures.length === 0;
  },

  /**
   * Sets the custom validation function which overrides the default standard validation.
   *
   * @param validationFunction The custom validation function
   */
  setValidationFunction: function (validationFunction) {
    this._validationFunction = validationFunction;
  },

  /**
   * Validate all fields in a form which have data-validation attributes on them describing how the field should be
   * validated.
   *
   * @param $form The form element with fields to validate
   * @param $triggeringElement The element that caused the validation to happen
   * @returns {array} of any validation failures found
   */
  standardValidation: function ($form, $triggeringElement) {
    "use strict";

    var validationFailures = [];

    var validatableElements = LITECommon.ClientSideValidation._findValidatableElements($form, $triggeringElement);

    validatableElements.each(function (i, field) {

      LITECommon.ClientSideValidation._clearFieldClientSideError(field);

      var validationRules = $(field).data('validation');
      var validator;

      if ('required' in validationRules) {
        validator = validationRules.required;
        if (LITECommon.ClientSideValidation._isTextField(field)) {
          // If the field is a text input, check the values length is above 0
          if (!$(field).val() || $(field).val().length <= 0) {
            // Ignore countrySelect select element - uses generated input element
            if (!LITECommon.ClientSideValidation._isSelectCountryField(field)) {
              validationFailures.push({field: field, message: validator.message});
            }
          }
        }
        else if ($("[name*='" + field.id + "']").is('input[type=checkbox], input[type=radio]')) {
          // If the field is a checkbox/radio input, check there are elements with that name marked as checked
          if ($("input[name*='" + field.id + "']:checked").length === 0) {
            validationFailures.push({field: field, message: validator.message});
          }
        }
      }

      if ('email' in validationRules) {
        validator = validationRules.email;
        // If the field has a value, fail if it isn't an @ surrounded by some non-@ characters
        if ($(field).val().length > 0 && !$(field).val().match(/[^@]+@[^@]+/)) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

      if ('pattern' in validationRules) {
        validator = validationRules.pattern;
        // If the field has a value, fail if it doesn't match against the provided pattern
        if ($(field).val().length > 0 && !$(field).val().match(validator.pattern)) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

      if ('max' in validationRules) {
        validator = validationRules.max;
        // If the field has a value, fail if it's greater than the limit
        if ($(field).val().length > 0 && $(field).val() > validator.limit) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

      if ('min' in validationRules) {
        validator = validationRules.min;
        // If the field has a value, fail if it's lower than the limit
        if ($(field).val().length > 0 && $(field).val() < validator.limit) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

      if ('maxLength' in validationRules) {
        validator = validationRules.maxLength;
        // If the field has a value, fail if its length is greater than the limit
        if ($(field).val().length > 0 && $(field).val().length > validator.limit) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

      if ('minLength' in validationRules) {
        validator = validationRules.minLength;
        // If the field has a value, fail if its length is lower than the limit
        if ($(field).val().length > 0 && $(field).val().length < validator.limit) {
          validationFailures.push({field: field, message: validator.message});
        }
      }

    });

    return validationFailures;
  },

  /**
   * Find the fields which are validated
   *
   * @param $form
   * @param $triggeringElement
   * @returns {Array}
   * @private
   */
  _findValidatableElements: function($form, $triggeringElement) {
    // Find grouped
    var validatableElements = [];
    var validationGroup = $triggeringElement.attr('data-validation-group');
    if (validationGroup) {
      validatableElements = $form.find("[data-validation-group='" + validationGroup + "']").filter('[data-validation]');
    }
    else {
      validatableElements = $form.find('[data-validation]').not('[data-validation-group]');
    }
    return validatableElements;
  },

  /**
   * Find the form-group for a validation field (which could be the 'field' itself in the case of group-input fields)
   *
   * @param field Field to find the form-group for
   * @returns {*|jQuery}
   * @private
   */
  _findFieldFormGroup: function (field) {
    "use strict";

    var formGroup = $(field).closest('div.form-group');

    // If this is a date part field, the form group for this field has an ancestor form group, which is the one we want to use
    if (LITECommon.ClientSideValidation._isDatePartField(field)) {
      formGroup = formGroup.parent().closest('div.form-group');
    }

    if (formGroup.length === 0) {
      throw "Form Group not found for field id=" + field.id + ", name=" + field.name;
    }

    return formGroup;
  },

  /**
   * Remove any client side errors already on the page from previous validation that are displayed for a given field
   *
   * @param field Field to remove any existing client side error messages from
   * @private
   */
  _clearFieldClientSideError: function (field) {
    "use strict";

    var $formGroup = LITECommon.ClientSideValidation._findFieldFormGroup(field);
    var $formGroupLegend = $formGroup.find('legend');
    var $errorMessages = $formGroup.find(".error-message");
    var $clientsideErrorMessages = $formGroup.find(".error-message["+LITECommon.ClientSideValidation.clientSideDataAttrName+"]");

    var onlyErrorsAreClientSide = ($errorMessages.length === $clientsideErrorMessages.length);

    $clientsideErrorMessages.remove();

    if (onlyErrorsAreClientSide) {
      $(field).removeClass('form-control-error');
    }
    else {
      return;
    }

    // Remove the error class, wherever it is
    $formGroup.find('.form-group-error').addBack().removeClass('form-group-error');
  },

  /**
   * Add an error message to a field
   *
   * @param field Field the validation failed on
   * @param message Validation failure message
   * @private
   */
  _addErrorMessageToField: function (field, message) {
    "use strict";

    var $formGroup = LITECommon.ClientSideValidation._findFieldFormGroup(field);

    // If there's already a matching error message for this field, don't add another
    if ($(".error-message:contains('" + message + "')", $formGroup).length > 0) {
      return;
    }

    var $formGroupLegend = $formGroup.find('legend');

    var $errorMessage = $("<span/>");
    $errorMessage.text(message);
    $errorMessage.addClass("govuk-error-message");
    $errorMessage.attr(LITECommon.ClientSideValidation.clientSideDataAttrName, true);

    if (LITECommon.ClientSideValidation._isTextField(field)) {
      $(field).addClass('form-control-error');
    }

    // If the field already contains an error message the form group already has the necessary styling,
    // so just put this error message after the existing one
    if ($formGroup.find('.error-message').length > 0) {
      $formGroup.find('.error-message:last').after($errorMessage);
      return;
    }

    // Add the error styling to the appropriate elements
    $formGroup
      .find(LITECommon.ClientSideValidation.formGroupErrorTargetSelector)
      .addBack(LITECommon.ClientSideValidation.formGroupErrorTargetSelector)
      .addClass('form-group-error');

    // Add the error message
    $formGroup
      .find(LITECommon.ClientSideValidation.errorMessageParentSelector)
      .addBack(LITECommon.ClientSideValidation.errorMessageParentSelector)
      .append($errorMessage);
  },

  /**
   * Add or update the error summary box typically at the top of the page
   *
   * @param validationFailures Array of objects containing a message and an HTML object field the message is associated with
   * @private
   */
  _addErrorSummary: function (validationFailures) {
    "use strict";

    if (validationFailures.length > 0) {

      // Add validation failure messages to the invalid fields
      validationFailures.forEach(function(item) {
        LITECommon.ClientSideValidation._addErrorMessageToField(item.field, item.message);
      });

      var errorSummary;
      // Look for existing summary
      var existingSummary = $('div.error-summary');
      if (existingSummary.length > 0) {
        errorSummary = existingSummary.first();
      }
      else {
        // Look for error summary anchor
        var anchor = $('a#error-summary-anchor');
        if (anchor.length === 0) {
          anchor = $('a#contentStart');
          if (anchor.length === 0) {
            throw "Unable to find a suitable anchor to create a new error summary after, skipping adding any error summary";
          }
        }
        // Create new summary and insert after anchor
        var newSummary = $('<div data-clientside-validation="true" class="error-summary" role="alert" aria-labelledby="error-summary-heading" tabindex="-1">' +
          '<h2 class="heading-medium error-summary-heading" id="error-summary-heading">There are errors on this page</h2>' +
          '<ul class="error-summary-list"></ul>' +
          '</div>');
        errorSummary = newSummary.insertAfter(anchor);
      }

      var errorSummaryList = $(errorSummary).find('ul.error-summary-list');
      // Clear any previous clientside messages first
      $(errorSummaryList).find("li["+LITECommon.ClientSideValidation.clientSideDataAttrName+"]").remove();
      // Add to error-summary-list if the message/id is not already there
      $(validationFailures).each(function (i, failure) {
        if ($('a[href="#' + failure.field.id + '"]:contains("' + failure.message + '")').length === 0) {
          errorSummaryList.append('<li '+LITECommon.ClientSideValidation.clientSideDataAttrName+'="true"><a href="#' + failure.field.id + '">' + failure.message + '</a></li>');
        }
      });

      $(errorSummary).focus();
    }
    else {
      // Clear client side if there's no errors this time
      $("div.error-summary["+LITECommon.ClientSideValidation.clientSideDataAttrName+"]").remove();
    }
  },

  /**
   * Add 'Error: ' to the start of the page's <title> element if there are errors, or remove it if there aren't
   *
   * @param validationFailures Array of objects containing a message and a HMTL object field the message is associated with
   * @private
   */
  _addErrorToPageTitle: function (validationFailures) {
    "use strict";

    if (validationFailures.length > 0 && document.title.indexOf('Error: ') !== 0) {
      document.title = 'Error: ' + document.title;
    }
    else if (validationFailures.length === 0 && document.title.indexOf('Error: ') === 0) {
      document.title = document.title.replace('Error: ', '');
    }
  },

  /**
   * Determines whether the field is textual
   *
   * @param field Field to check the type of
   * @returns {boolean}
   * @private
   */
  _isTextField: function (field) {
    "use strict";

    return $(field).is('textarea, select, input[type=color], input[type=date], input[type=datetime-local], input[type=email], input[type=month], input[type=number], input[type=password], input[type=range], input[type=search], input[type=tel], input[type=text], input[type=time], input[type=url], input[type=week]');
  },

  /**
   * Determines whether the field is select element from countrySelect
   *
   * @param field Field to check the type of
   * @returns {boolean}
   * @private
   */
  _isSelectCountryField: function (field) {
    "use strict";
    return $(field).is("select[name*='Country']");
  },

  /**
   * Determines whether the field is part of a three-input date
   *
   * @param field Field to check the type of
   * @returns {boolean}
   * @private
   */
  _isDatePartField: function (field) {
    "use strict";

    return $(field).closest('.form-date').length > 0;
  },

  /**
   * Determines whether the form group has a legend
   *
   * @param $formGroup
   * @returns {boolean}
   * @private
   */
  _formGroupHasLegend: function($formGroup) {
    "use strict";

    return $formGroup.find('legend').length > 0;
  }

};

// When the document is ready attach validation to forms with validating fields
$(document).ready(function (){
  $('form').each(function (i, form) {
    // For each form see if it has fields with validation attributes on them
    if ($(form).find('[data-validation]').length > 0) {
      // If so add a submit hook
      $(form).find('button[type=submit]').click(LITECommon.ClientSideValidation.onSubmitClick);
      $(form).submit(LITECommon.ClientSideValidation.handleSubmit);
    }
  });
});
