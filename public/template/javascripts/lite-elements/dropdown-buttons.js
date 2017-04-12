;(function (global) {
  'use strict'

  var $ = global.jQuery
  var GOVUK = global.GOVUK || {}

  var DropdownButtons = function (elmsOrSelector) {
    this.selectedClass = 'selected'
    this.focusedClass = 'focused'
    this.buttonClass = 'dropdown-button'
    this.contentClass = 'dropdown-button-content'

    if (typeof elmsOrSelector === 'string') {
      this.selector = elmsOrSelector
    } else if (elmsOrSelector !== undefined) {
      this.$elms = elmsOrSelector
    }
    this.addEvents()
  }
  DropdownButtons.prototype.addEvents = function () {
    if (typeof this.$elms !== 'undefined') {
      this.addElementLevelEvents()
    } else {
      this.addDocumentLevelEvents()
    }
  }
  DropdownButtons.prototype.markFocused = function ($elm, state) {
    if (state === 'focused') {
      $elm.parent('.' + this.buttonClass).addClass(this.focusedClass)
    } else {
      $elm.parent('.' + this.buttonClass).removeClass(this.focusedClass)
    }
  }
  DropdownButtons.prototype.markSelected = function ($elm) {
    if ($elm.is(':checked')) {
      $elm.parent('.' + this.buttonClass).addClass(this.selectedClass)
      $elm.attr('aria-expanded', true)
      $elm.siblings('.' + this.contentClass).attr('aria-hidden', false)
    } else {
      $elm.parent('.' + this.buttonClass).removeClass(this.selectedClass)
      $elm.attr('aria-expanded', false)
      $elm.siblings('.' + this.contentClass).attr('aria-hidden', true)
    }
  }
  DropdownButtons.prototype.focusLost = function ($elm) {
    var $button = $elm.closest('.' + this.buttonClass);

    var that = this;

    setTimeout(function() {
      if ($button.find('*:focus').length === 0) {
        $button.children('input[type=checkbox]').prop('checked', false);
        that.markSelected($button.children('input[type=checkbox]'));
      }
    }, 50);
  }
  DropdownButtons.prototype.addElementLevelEvents = function () {
    this.clickHandler = this.getClickHandler()
    this.focusHandler = this.getFocusHandler({'level': 'element'})
    this.focusOutHandler = this.getFocusOutHandler()

    this.$elms.children('input[type=checkbox]')
      .on('click', this.clickHandler)
      .on('focus blur', this.focusHandler)

    this.$elms.children()
      .on('focusout', this.getFocusOutHandler())
  }
  DropdownButtons.prototype.addDocumentLevelEvents = function () {
    this.clickHandler = this.getClickHandler()
    this.focusHandler = this.getFocusHandler({'level': 'document'})
    this.focusOutHandler = this.getFocusOutHandler()

    $(document)
      .on('click', this.selector, this.clickHandler)
      .on('focus blur', this.selector, this.focusHandler)
      .on('focusout', this.selector, this.focusOutHandler)
  }
  DropdownButtons.prototype.getClickHandler = function () {
    return function (e) {
      this.markSelected($(e.target))
    }.bind(this)
  }
  DropdownButtons.prototype.getFocusHandler = function (opts) {
    var focusEvent = (opts.level === 'document') ? 'focusin' : 'focus'

    return function (e) {
      var state = (e.type === focusEvent) ? 'focused' : 'blurred'
      this.markFocused($(e.target), state)
    }.bind(this)
  }
  DropdownButtons.prototype.getFocusOutHandler = function () {
    return function (e) {
      this.focusLost($(e.target))
    }.bind(this)
  }
  DropdownButtons.prototype.destroy = function () {
    if (typeof this.selector !== 'undefined') {
      $(document)
        .off('click', this.selector, this.clickHandler)
        .off('focus blur', this.selector, this.focusHandler)
    } else {
      this.$elms.children('input[type=checkbox]')
        .off('click', this.clickHandler)
        .off('focus blur', this.focusHandler)
    }
  }

  GOVUK.DropdownButtons = DropdownButtons
  global.GOVUK = GOVUK
})(window)
