(function (global) {
  'use strict'

  var $ = global.jQuery
  var GOVUK = global.GOVUK || {}

  var Clocks = function (elmsOrSelector, opts) {
    this.width = 40
    this.strokeWidth = 7
    this.dotWidth = 4
    this.trackColor = '#E9EBEB'
    this.dotColor = '#FFF'
    this.progressColors = { '0': '#00823B', '0.51': '#F47738', '0.75': '#DF3034', '1': '#0B0C0C' }
    if (opts !== undefined) {
      $.each(opts, function (optionName, optionObj) {
        this[optionName] = optionObj
      }.bind(this))
    }
    if (typeof elmsOrSelector === 'string') {
      this.selector = elmsOrSelector
      this.drawAll($(this.selector))
    } else if (elmsOrSelector !== undefined) {
      this.$elms = elmsOrSelector
      this.drawAll(this.$elms)
    }
  }
  Clocks.prototype.drawAll = function ($elms) {
    $elms.each(function (idx, elm) {
      var $elm = $(elm)
      this.draw($elm)
    }.bind(this))
  }
  Clocks.prototype.draw = function ($elm) {
    function polarToCartesian(centerX, centerY, radius, degrees) {
      var radians = (degrees-90) * Math.PI / 180.0;
      return {
        x: centerX + (radius * Math.cos(radians)),
        y: centerY + (radius * Math.sin(radians))
      }
    }

    function calculateProgressArc (x, y, radius, startAngle, endAngle) {
      var progressStart = polarToCartesian(x, y, radius, endAngle)
      var progressEnd = polarToCartesian(x, y, radius, startAngle)

      var largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1'

      var d = [
        "M", progressStart.x, progressStart.y,
        "A", radius, radius, 0, largeArcFlag, 0, progressEnd.x, progressEnd.y
      ].join(" ")

      return d
    }

    //Remove any SVG clock that's already been drawn
    $elm.find('svg').remove()
    $elm.removeClass('has-svg-clock')

    var center = this.width / 2
    var radius = (this.width / 2.0) - (this.strokeWidth / 2.0)
    var progressProportion = $elm.attr('data-clock-elapsed') / $elm.attr('data-clock-limit')
    var progressDegrees = progressProportion * 360
    var progressArc = calculateProgressArc(center, center, radius, 0, (progressDegrees >= 360) ? 359.9 : progressDegrees)
    var progressColor
    var lastKey = -1

    for (var key in this.progressColors) {
      if(this.progressColors.hasOwnProperty(key) && parseFloat(key) <= progressProportion && parseFloat(key) > lastKey) {
        progressColor = this.progressColors[key]
        lastKey = parseFloat(key)
      }
    }

    /*
     * Draw the SVG
     */
    var svgNs = 'http://www.w3.org/2000/svg'

    //SVG element
    var clock = document.createElementNS(svgNs, "svg")
    clock.setAttribute('width', this.width)
    clock.setAttribute('height', this.width)

    //Track
    var track = document.createElementNS(svgNs, "circle")
    track.setAttribute('cx', center)
    track.setAttribute('cy', center)
    track.setAttribute('r', radius)
    track.setAttribute('stroke', this.trackColor)
    track.setAttribute('stroke-width', this.strokeWidth - 2)
    track.setAttribute('fill-opacity', '0')

    //Progress arc
    var progress = document.createElementNS(svgNs, "path")
    progress.setAttribute('fill', 'none')
    progress.setAttribute('stroke', progressColor)
    progress.setAttribute('stroke-width', this.strokeWidth)
    progress.setAttribute('stroke-linecap', 'round')
    progress.setAttribute('d', progressArc)

    //Progress dot
    var dotGroup = document.createElementNS(svgNs, "g")
    dotGroup.setAttribute('transform', 'rotate(' + progressDegrees + ' ' + this.width / 2.0 + ' ' + this.width / 2.0 + ')')
    var dotRect = document.createElementNS(svgNs, "rect")
    dotRect.setAttribute('width', this.width)
    dotRect.setAttribute('height', this.width)
    dotRect.setAttribute('fill-opacity', 0)
    var dot = document.createElementNS(svgNs, "circle")
    dot.setAttribute('cx', this.width / 2.0)
    dot.setAttribute('cy', this.strokeWidth / 2.0)
    dot.setAttribute('r', this.dotWidth / 2.0)
    dot.setAttribute('fill', this.dotColor)
    dotGroup.appendChild(dotRect)
    dotGroup.appendChild(dot)

    clock.appendChild(track)
    clock.appendChild(progress)
    clock.appendChild(dotGroup)

    $elm.append(clock)
    $elm
      .width(this.width)
      .height(this.width)
      .css('line-height', (this.width * 1.1) + 'px')
      .css('text-align', 'center')
      .addClass('has-svg-clock')
  }

  GOVUK.Clocks = Clocks
  global.GOVUK = GOVUK
})(window)