(function (global) {
  'use strict'

  var $ = global.jQuery
  var GOVUK = global.GOVUK || {}

  var BulkActions = function (bulkActionId, afterSelectCallback, afterUnselectCallback, afterSelectAllCallback, afterUnselectAllCallback) {
    this.bulkActionId = bulkActionId
    this.checkboxesSelector = '.block-label:not(.select-all) input[data-bulk-action-id=' + this.bulkActionId + ']'
    this.selectAllSelector = '.block-label.select-all input[data-bulk-action-id=' + this.bulkActionId + ']'
    this.actionsSelector = 'button[data-bulk-action-id=' + this.bulkActionId + ']'
    this.paneSelector = '#' + this.bulkActionId
    this.checkedCheckboxesCount = 0;
    this.paneVisible = false;

    //Move the pane to be a direct child of body so we can get the pane and content to scroll independently
    var $detachedPane = $('#' + this.bulkActionId).detach()
    $detachedPane.appendTo("body")

    //Set up event handlers
    var that = this
    $('body').on('change', this.checkboxesSelector, function(e) {
      that.updateSelectAllState()
      that.updateCount()
      //that.togglePane()
      that.toggleActions()

      //ME POSSIBLY TEMP - close panel if nothing selected
      if($(that.checkboxesSelector).filter(':checked').length === 0) {
        that.togglePane();
      }
      //END ME POSSIBLY TEMP

      if($(e.target).is(':checked') && afterSelectCallback !== undefined) {
        afterSelectCallback($(e.target));
      } else if (afterUnselectCallback !== undefined) {
        afterUnselectCallback($(e.target));
      }
      e.stopPropagation()
    })
    $('body').on('change', this.selectAllSelector, function(e) {
      that.toggleSelectAll()
      that.updateCount()
      if($(e.target).is(':checked') && afterSelectAllCallback !== undefined) {
        afterSelectCallback($(e.target));
      } else if (afterUnselectAllCallback !== undefined) {
        afterUnselectCallback($(e.target));
      }
      e.stopPropagation()
    })
    $('body').on('click', '.cancel-bulk-action a', function(e) {
      that.cancelBulkAction(e)
    })
  }
  BulkActions.prototype.handleWindowResize = function() {
    $('body').css('transition', 'none')
    $(this.paneSelector).css('transition', 'none')
    this.updatePanePosition()
    $('body').css('transition', '')
    $(this.paneSelector).css('transition', '')
  }
  BulkActions.prototype.togglePane = function () {
    if($(this.checkboxesSelector).filter(':checked').length === 0){
      //If no rows are selected, hide the pane
      this.hidePane()
    } else {
      //If any rows are selected, show the pane
      this.showPane()
    }
  }
  BulkActions.prototype.showPane = function () {
    if(this.paneVisible) {
      return;
    }
    var bodyScrollPos = $('body').scrollTop();
    $(this.paneSelector).addClass('visible').attr('aria-hidden', 'false')
    if($(this.paneSelector).hasClass('right-bulk-actions-pane')){
      $('html').addClass('right-pane-visible')
    }
    this.updatePanePosition()
    this.hideStatus()
    $('#body-scroll').scrollTop(bodyScrollPos);
    $('body').scrollTop(0);
    this.paneVisible = true;
    var that = this
    $(window).on('resize.bulkactionresize', function(e) {
      that.handleWindowResize()
    })
  }
  BulkActions.prototype.hidePane = function () {
    if(!this.paneVisible) {
      return;
    }
    var bodyScrollPos = $('#body-scroll').scrollTop();
    $(this.paneSelector).removeClass('visible').attr('aria-hidden', 'true')
    $('body').css('transform', '')
    $('html').removeClass('right-pane-visible')
    $('#body-scroll').css('width', parseFloat($('body').outerWidth()) + 'px')
    setTimeout(function() { $('#body-scroll').css('width', '') }, 200)
    $(this.paneSelector).css('transform', '')
    $('body').scrollTop(bodyScrollPos);
    $('#body-scroll').scrollTop(0);
    this.paneVisible = false;
    var that = this
    setTimeout(function() { that.hideStatus() }, 200) //Only do this after 200ms so the panel is offscreen before we hide the status
    //Disable the window resize event so it's not firing unnecessarily
    $(window).off('resize.bulkactionresize')
  }
  BulkActions.prototype.toggleActions = function() {
    if($(this.checkboxesSelector).filter(':checked').length === 0){
      //If no rows are selected, disable the actions
      $(this.actionsSelector).attr('disabled', 'true')
    } else {
      //If any rows are selected, enable the actions
      $(this.actionsSelector).removeAttr('disabled')
    }
  }
  BulkActions.prototype.updatePanePosition = function () {
    //Dynamically position the pane so that it overlaps as little as possible
    if($(this.paneSelector).hasClass('right-bulk-actions-pane')){
      var paneWidth = parseFloat($(this.paneSelector).outerWidth())
      var windowWidth = $(window).width()
      //Explicitly set the body-scroll width otherwise the CSS transition doesn't work
      $('#body-scroll').css('width', parseFloat($('#body-scroll').outerWidth()) + 'px')
      setTimeout(function() { $('#body-scroll').css('width', windowWidth - paneWidth + 'px') }, 0) //This is in a timeout as the previous width doesn't get set otherwise
    }
  }
  BulkActions.prototype.toggleSelectAll = function () {
    var $inputs = $(this.checkboxesSelector)
    if($(this.selectAllSelector).prop('checked') === true) {
      $inputs.prop('checked', true).closest('.block-label').addClass('selected')
    } else {
      $inputs.prop('checked', false).closest('.block-label').removeClass('selected')
    }
    //this.togglePane()
    this.toggleActions()
    //ME POSSIBLY TEMP - close panel if nothing selected
    if($(this.checkboxesSelector).filter(':checked').length === 0) {
      this.togglePane();
    }
    //END ME POSSIBLY TEMP
  }
  BulkActions.prototype.updateSelectAllState = function () {
    var allCheckboxesCount = $(this.checkboxesSelector).length
    var checkedCheckboxesCount = $(this.checkboxesSelector).filter(':checked').length
    if(allCheckboxesCount === checkedCheckboxesCount) {
      $(this.selectAllSelector).prop('checked', true).closest('.block-label').addClass('selected')
    } else {
      $(this.selectAllSelector).prop('checked', false).closest('.block-label').removeClass('selected')
    }
  }
  BulkActions.prototype.updateCount = function () {
    this.checkedCheckboxesCount = $(this.checkboxesSelector).filter(':checked').length
    if(this.checkedCheckboxesCount !== 0) {
      $(this.paneSelector).find('.selected-item-count').text(this.checkedCheckboxesCount)
    }
    if(this.checkedCheckboxesCount == 1) {
      $(this.paneSelector).find('.selected-item-name-singular').show()
      $(this.paneSelector).find('.selected-item-name-plural').hide()
    } else {
      $(this.paneSelector).find('.selected-item-name-singular').hide()
      $(this.paneSelector).find('.selected-item-name-plural').show()
    }
  }
  BulkActions.prototype.cancelBulkAction = function (e) {
    $(this.checkboxesSelector).prop('checked', false).closest('.block-label').removeClass('selected')
    this.updateSelectAllState()
    this.updateCount()
    this.togglePane()
    e.preventDefault()
  }
  BulkActions.prototype.setStatusText = function (text) {
    $(this.paneSelector).find('.bulk-action-status-text').text(text)
  }
  BulkActions.prototype.showStatus = function () {
    $(this.paneSelector).find('.bulk-action-status').addClass('visible').attr('aria-hidden', 'false')
  }
  BulkActions.prototype.hideStatus = function () {
    $(this.paneSelector).find('.bulk-action-status').removeClass('visible').attr('aria-hidden', 'true')
  }

  GOVUK.BulkActions = BulkActions
  global.GOVUK = GOVUK
})(window)