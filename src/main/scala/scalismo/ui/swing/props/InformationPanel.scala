package scalismo.ui.swing.props

import javax.swing.border.TitledBorder

import scalismo.common.Scalar
import scalismo.geometry.{ Index, Point, Vector, _3D }
import scalismo.image.DiscreteScalarImage
import scalismo.mesh.TriangleMesh
import scalismo.ui.visualization.props.HasScalarRange
import scalismo.ui.{ Image3DView, MeshView, ScalarMeshFieldView }

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.swing.GridBagPanel.{ Anchor, Fill }
import scala.swing._

class InformationPanel extends BorderPanel with PropertyPanel {
  override def description: String = "Information"

  val providers: List[InformationProvider] = List(MeshInformationProvider, MeshFieldInformationProvider, ScalarRangeInformationProvider)
  
  var target: Option[AnyRef] = None

  override def setObject(obj: Option[AnyRef]): Boolean = {
    cleanup()
    obj match {
      case Some(thing) =>
        val applicable = providers.filter(_.supports(thing))
        if (applicable.nonEmpty) {
          target = Some(thing)
          updateUi(applicable)
          true
        } else false
      case None => false
    }
  }

  def cleanup(): Unit = {
    peer.removeAll()
  }

  def updateUi(infos: List[InformationProvider]): Unit = {
    if (target.isDefined) {
      val panels = infos map createPanel
      panels.fold(this) { case (parent, child) => parent.layout(child) = BorderPanel.Position.North; child }
    }
  }

  def createPanel(info: InformationProvider): BorderPanel = new BorderPanel {

    def label(text: String): Label = new Label(text) {
      horizontalAlignment = Alignment.Left
    }

    def limit(s: String, maxLength: Int): String = {
      // ellipses add 3 again, so don't uselessly trim
      if (s.length < maxLength + 3) s
      else {
        s"${s.substring(0, maxLength)}..."
      }
    }

    class InfoPanel extends GridBagPanel {
      def add(key: String, value: String, row: Int): Unit = {
        val kc = constraintsFor(0, row)
        val vc = constraintsFor(1, row)
        super.add(label(key), kc)
        super.add(label(limit(value, 40)), vc)
      }

      def constraintsFor(x: Int, y: Int): Constraints = {
        val c = new Constraints
        c.gridx = x
        c.gridy = y
        if (x == 0) {
          // key
          c.anchor = Anchor.LineEnd
          c.ipadx = 15
          c.weightx = 0.0
        } else {
          //value
          c.anchor = Anchor.LineStart
          c.fill = Fill.Horizontal
          c.weightx = 1.0
        }
        c
      }

    }

    val data = info.getInformation(target.get)
    val inner = new InfoPanel
    inner.border = new TitledBorder(info.title)
    data.zipWithIndex.foreach {
      case ((key, value), row) =>
        inner.add(key, value, row)
    }
    layout(inner) = BorderPanel.Position.Center
  }
}

trait InformationProvider {
  def title: String

  def supports(obj: AnyRef): Boolean

  def getInformation(obj: AnyRef): List[(String, String)]
}

abstract class TypedInformationProvider[T <: AnyRef: ClassTag] extends InformationProvider {

  import scala.language.implicitConversions

  private val clazz = implicitly[ClassTag[T]].runtimeClass

  final override def supports(obj: AnyRef): Boolean = {
    clazz.isInstance(obj)
  }

  final override def getInformation(obj: AnyRef): List[(String, String)] = {
    val t = obj.asInstanceOf[T]
    about(t)
  }

  def about(t: T): List[(String, String)]

  // round with a maximum of 2 after-comma digits
  private def r(f: Float): String = f"$f%01.02f"

  implicit def numberToString[V <: AnyVal](v: V): String = v.toString

  implicit def vector3DToString(v: Vector[_3D]): String = s"(${r(v(0))}, ${r(v(1))}, ${r(v(2))})"

  implicit def point3DToString(v: Point[_3D]): String = s"(${r(v(0))}, ${r(v(1))}, ${r(v(2))})"

  implicit def index3DToString(v: Index[_3D]): String = s"(${v(0)}, ${v(1)}, ${v(2)})"

}

object MeshInformationProvider extends TypedInformationProvider[MeshView] {
  override val title = "Mesh"

  override def about(t: MeshView): List[(String, String)] = meshInfo(t.source)

  def meshInfo(t: TriangleMesh): List[(String, String)] = {
    List("Number of points" -> t.points.toSeq.length, "Number of cells" -> t.cells.length)
  }
}

object MeshFieldInformationProvider extends TypedInformationProvider[ScalarMeshFieldView] {
  override val title = "Mesh"

  override def about(t: ScalarMeshFieldView): List[(String, String)] = MeshInformationProvider.meshInfo(t.source.mesh)
}

object ScalarRangeInformationProvider extends TypedInformationProvider[HasScalarRange] {

  override def title: String = "Scalar Values"

  override def about(t: HasScalarRange): List[(String, String)] = {
    List("Minimum" -> t.scalarRange.value.absoluteMinimum, "Maximum" -> t.scalarRange.value.absoluteMaximum)
  }
}