var LITECommon = LITECommon || {};

LITECommon.Modal = {
	modalBackground: {},
	modal: {},
	container: {},
	backButton: {},
	closeButton: {},

	showModal: function(title, content) {
		// Generate modal container if it doesn't exist
		if (!$("#modal-background").length) {
			$("body").append("<div id='modal-background'><div id='modal'><div id='modal-header'><a id='modal-back-button' class='govuk-link' href='#'>Back</a><a id='modal-close-button' class='govuk-link' href='#'>Close</a></div><div id='modal-contents'></div></div></div>")

			// Bind
			this.modalBackground = $("#modal-background");
			this.modal = $("#modal");
			this.container = $("#modal-contents");
			this.backButton = $("#modal-back-button");
			this.closeButton =  $("#modal-close-button");
		}

		// Show modal
		this.modalBackground.show();
		$("html, body").addClass("has-modal");

		// Hide existing modal content and show back button if necessary
		if ($(".modal-content").length > 0) {
			this.backButton.text("Back to " + $(".modal-content").last().children($("h2")).first().text());
			$(".modal-content").hide();
			this.backButton.show();
		}

		// Append
		this.container.append("<div class='modal-content'>" + "<h2>" + title + "</h2>" + content + "</div>");

		// Bind events
    	LITECommon.Modal.bindEvents();
	},

	closeTopModal: function() {
		$(".modal-content").last().remove();
		$(".modal-content").last().show();

		if ($(".modal-content").length == 1) {
			this.backButton.hide();
		} else {
			// Get element before last element
			this.backButton.text("Back to " + $(".modal-content").eq(-2).children($("h2")).first().text());
		}
	},

	closeAllModals: function() {
		this.modalBackground.remove();
		$("html, body").removeClass("has-modal");
	},

	bindEvents: function() {
		"use strict";

		// Keydown events
		$('body').off('keydown.LITECommon.Modal').on('keydown.LITECommon.Modal', function(e) {
			// Escape key closes all modals
			if (e.which === 27) {
				LITECommon.Modal.closeAllModals();
			}
		});

		LITECommon.Modal.backButton.off('click').on("click", function() {
			LITECommon.Modal.closeTopModal();
			return false;
		});

		LITECommon.Modal.closeButton.off('click').on("click", function() {
			LITECommon.Modal.closeAllModals();
			return false;
		});
	}
};
