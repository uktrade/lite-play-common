var LITECommon = LITECommon || {};

LITECommon.initialiseSelectionButtons = function () {
  // Use GOV.UK selection-buttons.js to set selected
  // and focused states for block labels
  var $blockLabels = $(".block-label input[type='radio'], .block-label input[type='checkbox']")
  new GOVUK.SelectionButtons($blockLabels) // eslint-disable-line

  // Where .block-label uses the data-target attribute
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

$(document).ready(LITECommon.initialiseSelectionButtons())