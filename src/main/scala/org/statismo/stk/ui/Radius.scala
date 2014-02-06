package org.statismo.stk.ui

import scala.swing.Publisher
import org.statismo.stk.core.geometry.Point3D
import scala.swing.event.Event

object Radius {
  case class RadiusChanged(source: Radius) extends Event
}

trait Radius extends Publisher {
  
	private var _radius: Float = 0
	def radius = {_radius}
	def radius_=(newRadius: Float) = {
	  if (newRadius != _radius) {
	    _radius = newRadius
	    publish(Radius.RadiusChanged(this))
	  }
	}
}