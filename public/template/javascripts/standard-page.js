function back() {
  window.history.back();
}

var backButton = document.getElementById("back-button");

if(backButton) {
  if (backButton.addEventListener) {
      // For all major browsers, except IE 8 and earlier
      backButton.addEventListener("click", back);
  } else if (backButton.attachEvent) {
      // For IE 8 and earlier versions
      backButton.attachEvent("onclick", back);
  }
}
