function isIE( version, comparison ){
  var $div = $('<div style="display:none;"/>').appendTo($('body'));
  $div.html('<!--[if '+(comparison||'')+' IE '+(version||'')+']><a>&nbsp;</a><![endif]-->');
  var ieTest = $div.find('a').length;
  $div.remove();
  return ieTest;
}

//Attach event listener to the upload link for keyboard accessibility
$('.file-upload-button').on('keydown', function(e){
  //Enter or Space
  if(e.keyCode==13 || e.keyCode==32) {
    /*
     IE<10 don't actually upload the file when the click() event is called programmatically,
     and Firefox doesn't allow us to call the click event, so we focus on the file input
     and the keydown gets forwarded there. In IE this activates it if the
     space bar was pressed, but not if the enter key was pressed, because it focuses the textbox
     part of the file widget rather than the button part
     */
    if(isIE(9,'lte') || navigator.userAgent.toLowerCase().indexOf('firefox') > -1) {
      $(this).siblings('input[type=file]').focus();
    }
    //Webkit browsers handle the click event as expected
    else {
      $(this).siblings('input[type=file]').click();
    }

    e.preventDefault();
  }
});

function hideFileListIfEmpty() {
  if($('#file-list tbody tr').length === 0) {
    $('#file-list').addClass('empty-file-list');
  };
}

$(function () {
  $('#fileupload').fileupload({
    dataType: 'json',
    add: function (e, data) {
      var filename =  data.files[0].name;
      var size = data.files[0].size;
      var fileRowCount = $('#file-list tr').length;

      //Create tr in the uploads table
      data.context = $('<tr><th class="file-upload-filename bold" id="file-upload-filename-' + fileRowCount + '">' + filename + '</th><td class="file-upload-status"></td><td class="file-upload-actions"><a href="#" class="file-cancel-link">Cancel<span class="visually-hidden"> upload of ' + filename + '</span></a></td></tr>').prependTo($('#file-list tbody'));

      if (filename.endsWith('exe') || size > 5 * 1024 * 1024) { //TODO: improve client side validation (this is just an example)
        data.context.find('.file-upload-status').text('Error: Not a valid file');
        data.context.find('.file-upload-filename').addClass('form-group-error');
        data.context.find('.file-cancel-link').click(function(){
          //Remove the row from the table
          data.context.remove();
          hideFileListIfEmpty();
          return false;
        });
      } else {
        data.context.find('.file-upload-status').append('<span class="file-upload-progress" role="progressbar" aria-labelledby="file-upload-filename-' + fileRowCount + '" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"><span class="file-upload-progress-bar"></span></span>');
        data.context.find('.file-cancel-link').click(function(){
          //Cancel the xhr and remove the row from the table
          data.abort();
          data.context.remove();
          hideFileListIfEmpty();
          return false;
        });
        data.submitTimestamp = new Date().getTime();
        data.submit(); //starts the upload
      }
      $('#file-list').removeClass('empty-file-list');
    },
    progress : function (e, data) {
      var progress = parseInt(data.loaded / data.total * 100, 10);
      data.context.find('.file-upload-progress').attr('aria-valuenow', progress);
      data.context.find('.file-upload-progress-bar').css('width', progress + '%');
    },
    done: function (e, data) {
      var error = data.result.files[0].error;
      var size = data.result.files[0].size;
      var filename = data.result.files[0].name;
      var url = data.result.files[0].url;
      var param1 = data.result.files[0].param1;
      var param2 = data.result.files[0].param2;
      var param3 = data.result.files[0].param3;

      if (error) {
        data.context.find('.file-upload-status').text('Error:' + error);
      } else {
        var doneTimestamp = new Date().getTime();

        setTimeout( function() {
          //Add download link, success label and filesize
          data.context.find('.file-upload-filename').html('<a href="' + url + '"><span class="visually-hidden">Download</span> ' + filename + '</a>');
          data.context.find('.file-upload-status').html('<span class="status-label status-label-finished">Success</span><span class="file-upload-size">' + size + '</span>');

          // After 2 seconds, fade out the success label to reveal the file size
          setTimeout(function () {
            data.context.find('.file-upload-status .status-label').addClass('status-label-hidden');
          }, 2000);

          // Create the delete action
          data.context.find('.file-upload-actions').html('<a href="#" data-param1="' + param1 +
            '" data-param2="' + param2 +
            '" data-param3="' + param3 +
            '" class="file-delete-link">Remove<span class="visually-hidden"> ' + filename + '</span></a>');

          // If the page has been validated, remove the error summary and any error from the upload widget,
          // and rerun the validation
          if($("div.error-summary").length > 0) {
            $('#file-upload-container').removeClass('form-group-error').find('.error-message').remove();
            $("div.error-summary").remove();
            LITECommon.ClientSideValidation.validateForm($('#file-upload-container').closest('form'), $('#fileupload'));
          }
        }, 1000 - (doneTimestamp - data.submitTimestamp)); //Ensure the progress bar displays for at least one second
      }
    },
    fail: function (e, data) {
      data.context.find('.file-upload-status').text('Error:' + data.errorThrown);
      data.context.find('.file-upload-filename').addClass('form-group-error');
    },
    dropZone: $('.file-upload-drop-target')
  });
});

//Highlight dropzone when hovering over it (or its children)
$(document).bind('dragover', function (e) {
  var dropZone = $('.file-upload-drop-target');
  var found = false;
  var node = e.target;
  do {
    if (node === dropZone[0]) {
      found = true;
      break;
    }
    node = node.parentNode;
  } while (node != null);
  if (found) {
    dropZone.addClass('hover');
  } else {
    dropZone.removeClass('hover');
  }
});

//Disable tabbing to the file input if js is enabled
$('#fileupload').attr('tabindex', '-1');

//Bind click events to file delete links
$('body').on('click', '.file-delete-link', function(e){
  var param1 = $(this).attr('data-param1');
  var param2 = $(this).attr('data-param2');
  var param3 = $(this).attr('data-param3');
  var $fileRow = $(this).closest('tr');
  $fileRow.hide();

  $.ajax({
    url: $('input[name=delete-url]').val(),
    method: 'POST',
    headers: {
      "Csrf-Token": $('input[name=csrfToken]').val()
    },
    data: {
      param1: param1,
      param2: param2,
      param3: param3,
    },
    success: function() {
      $fileRow.remove();
      hideFileListIfEmpty();
    },
    error: function() {
      $fileRow.find('.file-upload-status').html('The file couldn\'t be removed');
      $fileRow.find('.file-upload-filename').addClass('form-group-error');
      $fileRow.show();
    }
  });

  return false;
});
