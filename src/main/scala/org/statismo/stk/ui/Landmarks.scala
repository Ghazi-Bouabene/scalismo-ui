package org.statismo.stk.ui

import java.io.File

import scala.swing.event.Event
import scala.util.Try

import org.statismo.stk.core.geometry.ThreeD
import org.statismo.stk.core.io.LandmarkIO
import scala.collection.immutable

import breeze.linalg.DenseVector
import org.statismo.stk.ui.visualization._
import org.statismo.stk.core.geometry.Point3D
import scala.Some
import scala.Tuple2
import org.statismo.stk.ui.visualization.props.{RadiusProperty, ColorProperty, OpacityProperty}

trait Landmark extends Nameable with Removeable {
  def point: Point3D
}

class ReferenceLandmark(val point: Point3D) extends Landmark

object VisualizableLandmark extends SimpleVisualizationFactory[VisualizableLandmark] {
  visualizations += Tuple2(Viewport.ThreeDViewportClassName, Seq(new ThreeDVisualizationAsSphere(None)))

  class ThreeDVisualizationAsSphere(from: Option[ThreeDVisualizationAsSphere]) extends Visualization[VisualizableLandmark] with SphereLike {
    override val color:ColorProperty = if (from.isDefined) from.get.color.derive() else new ColorProperty
    override val opacity:OpacityProperty = if (from.isDefined) from.get.opacity.derive() else new OpacityProperty
    override val radius:RadiusProperty = if (from.isDefined) from.get.radius.derive() else new RadiusProperty


    override protected def createDerived() = new ThreeDVisualizationAsSphere(Some(this))

    override protected def instantiateRenderables(source: VisualizableLandmark) = immutable.Seq(new SphereRenderable(source, color, opacity, radius))
  }

  class SphereRenderable(source: VisualizableLandmark, override val color: ColorProperty, override val opacity: OpacityProperty, override val radius: RadiusProperty) extends Renderable with SphereLike {
    setCenter()
    listenTo(source)
    reactions += {
      case Landmarks.LandmarkChanged(_) => setCenter()
    }

    def setCenter(): Unit = {
      center = source.point
    }
  }
}

abstract class VisualizableLandmark(container: VisualizableLandmarks) extends SceneTreeObject with Landmark with Visualizable[VisualizableLandmark]{
  override def parent = container
  override def parentVisualizationProvider = container
}

class StaticLandmark(initialCenter: Point3D, container: StaticLandmarks) extends VisualizableLandmark(container) {
  var point = initialCenter
}

class MoveableLandmark(container: MoveableLandmarks, source: ReferenceLandmark) extends VisualizableLandmark(container) {
  name = source.name
  listenTo(container.instance.meshRepresentation, source)

  override def remove() = {
    // we simply forward the request to the source, which in turn publishes an event that all attached
    // moveable landmarks get. And only then we invoke the actual remove functionality (in the reactions below)
    source.remove()
  }

  reactions += {
    case Mesh.GeometryChanged(m) => setCenter()
    case Nameable.NameChanged(n) =>
      if (n == source) {
        this.name = source.name
      } else if (n == this) {
        source.name = this.name
      }
    case Removeable.Removed(r) =>
      if (r eq source) {
        container.remove(this, silent = true)
      }
  }

  var point = calculateCenter()

  def calculateCenter(): Point3D = {
    val coeffs = DenseVector(container.instance.coefficients.toArray)
    source.point + container.instance.shapeModel.gaussianProcess.instance(coeffs)(source.point)
  }

  def setCenter(): Unit = {
    point = calculateCenter()
    publish(Landmarks.LandmarkChanged(this))
  }

}

object Landmarks extends FileIoMetadata {

  case class LandmarksChanged(source: AnyRef) extends Event
  case class LandmarkChanged(source: Landmark) extends Event

  override val description = "Landmarks"
  override val fileExtensions = Seq("csv")
}

trait Landmarks[L <: Landmark] extends MutableObjectContainer[L] with EdtPublisher with Saveable with Loadable {
  val saveableMetadata = Landmarks
  val loadableMetadata = Landmarks

  override def isCurrentlySaveable: Boolean = !children.isEmpty

  def create(peer: Point3D, name: Option[String]): Unit

  override def add(lm: L): Unit = {
    super.add(lm)
    publish(Landmarks.LandmarksChanged(this))
  }

  override def remove(lm: L, silent: Boolean) = {
    val changed = super.remove(lm, silent)
    if (changed) publish(Landmarks.LandmarksChanged(this))
    changed
  }

  override def saveToFile(file: File): Try[Unit] = {
    val seq = children.map {
      lm => (lm.name, lm.point)
    }.toIndexedSeq
    LandmarkIO.writeLandmarks[ThreeD](file, seq)
  }

  override def loadFromFile(file: File): Try[Unit] = {
    this.removeAll()
    val status = for {
      saved <- LandmarkIO.readLandmarks3D(file)
      newLandmarks = {
        saved.map {
          case (name, point) =>
            this.create(point, Some(name))
        }
      }
    } yield {}
    publish(Landmarks.LandmarksChanged(this))
    status
  }
}

abstract class VisualizableLandmarks(theObject: ThreeDObject) extends StandaloneSceneTreeObjectContainer[VisualizableLandmark] with Landmarks[VisualizableLandmark] with VisualizationProvider[VisualizableLandmark] with RemoveableChildren {
  name = "Landmarks"
  override lazy val isNameUserModifiable = false
  override lazy val parent = theObject

  def addAt(position: Point3D)

  override def parentVisualizationProvider = VisualizableLandmark
}

class ReferenceLandmarks(val shapeModel: ShapeModel) extends Landmarks[ReferenceLandmark] {
  lazy val nameGenerator: NameGenerator = NameGenerator.defaultGenerator

  def create(template: ReferenceLandmark): Unit = {
    create(template.point, Some(template.name))
  }

  def create(peer: Point3D, name: Option[String] = None): Unit = {
    val lm = new ReferenceLandmark(peer)
    lm.name = name.getOrElse(nameGenerator.nextName)
    add(lm)
  }
}

class StaticLandmarks(theObject: ThreeDObject) extends VisualizableLandmarks(theObject) {
  lazy val nameGenerator: NameGenerator = NameGenerator.defaultGenerator

  def addAt(peer: Point3D) = create(peer)

  def create(peer: Point3D, name: Option[String] = None): Unit = {
    val lm = new StaticLandmark(peer, this)
    lm.name = name.getOrElse(nameGenerator.nextName)
    add(lm)
  }

}

class MoveableLandmarks(val instance: ShapeModelInstance) extends VisualizableLandmarks(instance) {
  val peer = instance.shapeModel.landmarks

  def addAt(peer: Point3D) = {
    create(peer, None)
  }

  def create(peer: Point3D, name: Option[String]): Unit = {
    val index = instance.meshRepresentation.peer.findClosestPoint(peer)._2
    val refPoint = instance.shapeModel.peer.mesh.points(index).asInstanceOf[Point3D]
    instance.shapeModel.landmarks.create(refPoint, name)
  }

  listenTo(peer)

  reactions += {
    case Landmarks.LandmarksChanged(source) =>
      if (source eq peer) {
        syncWithPeer()
      }
  }

  syncWithPeer()

  def syncWithPeer() = {
    var changed = false
    _children.length until peer.children.length foreach {
      i =>
        changed = true
        add(new MoveableLandmark(this, peer(i)))
    }
    if (changed) {
      publish(SceneTreeObject.ChildrenChanged(this))
    }
  }

  override def remove() {
    deafTo(peer)
    super.remove()
  }
}