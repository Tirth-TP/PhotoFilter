package com.edit.imageproccessing.filters

data class Highlight(
  var black: Float = .1f,
  var white: Float = .7f
) : Filter()