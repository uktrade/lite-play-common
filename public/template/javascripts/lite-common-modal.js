/*jshint laxcomma: true, laxbreak: true, strict: false, devel: true */
/*global $:false, jQuery:false */

var LITECommon = LITECommon || {};
LITECommon.Modal = {
  overlayID: 'modal-overlay',
  contentID: 'modal-content',
  htmlClass: 'has-modal',
  closeClass: 'close-modal',
  focusableElementsString: 'a[href], area[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]',
  $originalFocusedElement: null,

  /**
   * Displays a modal onscreen containing the content in $content
   *
   * @param $content The jQuery-wrapped element to display in the modal
   * @param ariaLabel The label for the modal, read out by screen readers but not shown onscreen
   */
  displayModal: function($content, ariaLabel) {
    "use strict";

    // Store reference to currently focused element
    LITECommon.Modal.$originalFocusedElement = $(document.activeElement);

    // Remove any existing modal
    LITECommon.Modal.closeModal();

    // Build the new modal
    var $modal = $('<div id="' + LITECommon.Modal.overlayID + '">' +
      '<div id="modal" role="dialog" aria-label="' + ariaLabel + '" aria-describedby="modal-aria-description">' +
      '<div id="' + LITECommon.Modal.contentID + '" role="document">' +
      '</div></div></div>');
    $modal.find('#'+LITECommon.Modal.contentID).append('<div id="modal-aria-description" class="visually-hidden">Press escape to close this popover</div>');
    $modal.find('#'+LITECommon.Modal.contentID).append('<button id="close-modal-fixed-button" class="' + LITECommon.Modal.closeClass + ' link">Close<span class="visually-hidden"> popover</span></a>');
    $modal.find('#'+LITECommon.Modal.contentID).append($content);

    // Add the class that stops scrolling of the main page
    $('html').addClass(LITECommon.Modal.htmlClass);

    // Add the modal to the page
    $('body').append($modal);
    LITECommon.Modal.bindEvents();
    LITECommon.Modal.focusOnFirstElement();
  },

  /**
   * Close the modal
   * @param event click event that triggered the close
   */
  closeModal: function(event) {
    "use strict";

    $('#' + LITECommon.Modal.overlayID).remove();
    $('html').removeClass(LITECommon.Modal.htmlClass);
    LITECommon.Modal.unbindEvents();

    // Refocus on element that was focused when the modal opened (probably the link/button that triggered it)
    LITECommon.Modal.$originalFocusedElement.get(0).focus();
  },

  /**
   * Sets focus to the first focusable element inside the modal
   */
  focusOnFirstElement: function () {
    "use strict";

    var $elms = LITECommon.Modal.getFocusableElements();

    if ($elms.length > 0) {
      $elms[0].focus();
    }
  },

  /**
   * Binds events associated with the modal
   * @private
   */
  bindEvents: function() {
    "use strict";

    // Keydown events
    $('body').on('keydown.LITECommon.Modal', function(e) {
      // Escape key closes modal
      if (e.which === 27) {
        LITECommon.Modal.closeModal();
      }

      // Trap tab key
      if (e.which === 9) {
        LITECommon.Modal.trapTabKey(e);
      }
    });

    // Lets consumer define close links/buttons by giving them the class defined by closeClass
    $('body').on('click.LITECommon.modal', '.'+LITECommon.Modal.closeClass, LITECommon.Modal.closeModal);
  },

  /**
   * Unbinds all events associated with this modal set by bindEvents()
   * @private
   */
  unbindEvents: function() {
    "use strict";

    $('body').off('.LITECommon.Modal');
  },

  /**
   * Ensures focus stays within the modal by forcing tab key to loop through elements in the modal
   * @param event keydown event
   * @private
   */
  trapTabKey: function(event) {
    "use strict";

    var $focusableElms = LITECommon.Modal.getFocusableElements();
    var $focusedElm = $(document.activeElement);

    var focusedElmIndex = $focusableElms.index($focusedElm);

    if (event.shiftKey && focusedElmIndex === 0) {
      // If we're going backwards (shift-tab) and we're at the first focusable element,
      // loop back to last focusable element
      $focusableElms.get($focusableElms.length-1).focus();
      event.preventDefault();
    } else if(!event.shiftKey && focusedElmIndex === $focusableElms.length-1) {
      // If we're going forwards and we're at the last focusable element,
      // loop forwards to the first focusable element
      $focusableElms.get(0).focus();
      event.preventDefault();
    }
    // Otherwise, allow the tab to proceed as normal because we're still in the group of
    // focusable elements in the modal
  },

  /**
   * Finds all of the focusable elements inside the modal
   * @returns {jQuery} collection of the focusable elements inside the modal
   * @private
   */
  getFocusableElements: function() {
    "use strict";

    return $('#' + LITECommon.Modal.contentID).find('*').filter(LITECommon.Modal.focusableElementsString).filter(':visible');
  }
};