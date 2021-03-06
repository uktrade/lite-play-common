var LITECommon = LITECommon || {};

LITECommon.initialiseSelectionButtons = function () {
  // Where radio buttons or checkboxes use the data-target attribute
  // to toggle hidden content
  var showHideContent = new GOVUK.ShowHideContent()
  showHideContent.init()
};

// GOVUK compatible methods for showing and hiding content
LITECommon.showContent = function($content) {
  if ($content.hasClass('js-hidden')) {
    $content.removeClass('js-hidden');
    $content.attr('aria-hidden', 'false');
  }
};

LITECommon.hideContent = function($content) {
  if (!$content.hasClass('js-hidden')) {
    $content.addClass('js-hidden');
    $content.attr('aria-hidden', 'true');
  }
};

LITECommon.countrySelectInitialise = function($countrySelect) {
  $countrySelect.selectToAutocomplete({"autoFocus":false});
  LITECommon.reassociateAutocompleteInput($countrySelect);
};

LITECommon.reassociateAutocompleteInput = function($selectElement) {
  // Associates the new ui-autocomplete input with the original select id (if the input was created), needed for labels and such.
  var id = $($selectElement).attr("id");
  var autocompleteInput = $("input[data-ui-autocomplete-id=" + id + "]");
  if (autocompleteInput.length > 0) {
    autocompleteInput.attr("id", id);
    $selectElement.removeAttr("id");
  }
}

$(document).ready(LITECommon.initialiseSelectionButtons())