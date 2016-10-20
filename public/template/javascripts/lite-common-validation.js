/*jshint laxcomma: true, laxbreak: true, strict: false, devel: true */
/*global $:false, jQuery:false */

var LITECommon = LITECommon || {};
LITECommon.ClientSideValidation = {
  clientSideDataAttrName: 'data-clientside-validation',

  /**
   * Validate all fields in a form which have data-validation attributes on them describing how the field should be
   * validated.
   * This method should be raised by the form on submit so that the event parameter target will be the form containing
   * fields with validation information.
   *
   * @param event form submit event
   * @returns {boolean} True if all fields in the form passed validation
   */
  validateForm: function (event) {
    "use strict";

    var validationFailures = [];

    var form = event.target;

    $('[data-validation]', form).each(function (i, field) {
      LITECommon.ClientSideValidation._clearFieldClientSideError(field);

      var validationRules = $(field).data('validation');
      var validator;

      if ('required' in validationRules) {
        validator = validationRules.required;
        if ($(field).is('textarea, input[type=color], input[type=date], input[type=datetime-local], input[type=email], input[type=month], input[type=number], input[type=password], input[type=range], input[type=search], input[type=tel], input[type=text], input[type=time], input[type=url], input[type=week]')) {
          // If the field is a text input, check the values length is above 0
          if ($(field).val().length <= 0) {
            validationFailures.push({field: field, message: validator.message});
            LITECommon.ClientSideValidation._addError(field, validator.message);
          }
        }
        else if ($("[name='" + field.id + "']").is('input[type=checkbox], input[type=radio]')) {
          // If the field is a checkbox/radio input, check there are elements with that name marked as checked
          if ($("input[name='" + field.id + "']:checked").length === 0) {
            validationFailures.push({field: field, message: validator.message});
            LITECommon.ClientSideValidation._addError(field, validator.message);
          }
        }
      }

      if ('email' in validationRules) {
        validator = validationRules.email;
        // If the field has a value, check it is an @ surrounded by some non-@ characters
        if ($(field).val().length > 0 && !$(field).val().match(/[^@]+@[^@]+/)) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

      if ('pattern' in validationRules) {
        validator = validationRules.pattern;
        // If the field has a value, check it matches against the provided pattern
        if ($(field).val().length > 0 && $(field).val().match(validator.pattern)) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

      if ('max' in validationRules) {
        validator = validationRules.max;
        // If the field has a value, check it is lower than or equal to the limit
        if ($(field).val().length > 0 && $(field).val() <= validator.limit) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

      if ('min' in validationRules) {
        validator = validationRules.min;
        // If the field has a value, check it is greater than or equal to the limit
        if ($(field).val().length > 0 && $(field).val() >= validator.limit) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

      if ('maxLength' in validationRules) {
        validator = validationRules.maxLength;
        // If the field has a value, check its length is lower than or equal to the limit
        if ($(field).val().length > 0 && $(field).val().length <= validator.limit) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

      if ('minLength' in validationRules) {
        validator = validationRules.minLength;
        // If the field has a value, check its length is greater than or equal to the limit
        if ($(field).val().length > 0 && $(field).val().length >= validator.limit) {
          validationFailures.push({field: field, message: validator.message});
          LITECommon.ClientSideValidation._addError(field, validator.message);
        }
      }

    });

    LITECommon.ClientSideValidation._addErrorSummary(validationFailures);

    return validationFailures.length === 0;
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

    var formGroup = LITECommon.ClientSideValidation._findFieldFormGroup(field);
    var errorMessages = $("p.error-message", formGroup);
    var clientsideErrorMessages = $("p.error-message["+LITECommon.ClientSideValidation.clientSideDataAttrName+"]", formGroup);

    // Clear the error class on the form-group if the only messages in the group are client side ones
    if (errorMessages.length === clientsideErrorMessages.length) {
      formGroup.removeClass('error');
    }

    // Remove the client side messages
    clientsideErrorMessages.remove();
  },

  /**
   * Add an error message to a field
   *
   * @param field Field the validation failed on
   * @param message Validation failure message
   * @private
   */
  _addError: function (field, message) {
    "use strict";

    var formGroup = LITECommon.ClientSideValidation._findFieldFormGroup(field);
    if (!formGroup.hasClass('error')) {
      formGroup.addClass('error');
    }

    // Only add the message if there's not already a matching error message for this field
    if ($("p.error-message:contains('" + message + "')", formGroup).length === 0) {
      var errorMessage = $("<p/>");
      errorMessage.text(message);
      errorMessage.addClass("error-message");
      errorMessage.attr(LITECommon.ClientSideValidation.clientSideDataAttrName, true);

      if (field === formGroup[0]) {
        // If the 'field' is the form-group put the message at the top of the form-group content
        $(formGroup).prepend(errorMessage);
      }
      else {
        // If the 'field' is an individual field put the message before it
        $(field).before(errorMessage);
      }
    }
  },

  /**
   * Add or update the error summary box typically at the top of the page
   *
   * @param validationFailures Array of objects containing a message and a HMTL object field the message is associated with
   * @private
   */
  _addErrorSummary: function (validationFailures) {
    "use strict";

    if (validationFailures.length > 0) {
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
        var newSummary = $('<div data-clientside-validation="true" class="error-summary" role="group" aria-labelledby="error-summary-heading" tabindex="-1">' +
          '<h2 class="heading-medium error-summary-heading" id="error-summary-heading">There are errors on this page</h2>' +
          '<ul class="error-summary-list"></ul>' +
          '</div>');
        errorSummary = newSummary.insertAfter(anchor);
      }

      var errorSummaryList = $('ul.error-summary-list', errorSummary);
      // Clear any previous clientside messages first
      $("li["+LITECommon.ClientSideValidation.clientSideDataAttrName+"]", errorSummaryList).remove();
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
  }
};

// When the document is ready attach validation to forms with validating fields
$(document).ready(function (){
  $('form').each(function (i, form) {
    // For each form see if it has fields with validation attributes on them
    if ($('[data-validation]', form).length > 0) {
      // If so add a submit hook
      $(form).submit(LITECommon.ClientSideValidation.validateForm);
    }
  });
});
