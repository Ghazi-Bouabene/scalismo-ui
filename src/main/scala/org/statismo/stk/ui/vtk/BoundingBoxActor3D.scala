package org.statismo.stk.ui.vtk

import vtk.vtkSphereSource
import org.statismo.stk.ui.visualization.{VisualizationProperty, SphereLike}
import org.statismo.stk.ui.{BoundingBox, Scene}

class BoundingBoxActor3D(source: Scene.SlicingPosition.BoundingBoxRenderable3D) extends PolyDataActor {
  val scene = source.source.scene

  this.GetProperty().SetColor(1,0,0)
  listenTo(scene)
  update(false)

  reactions += {
    case Scene.SlicingPosition.BoundingBoxChanged(s) => update()
  }

  def update(withEvent: Boolean = true) = this.synchronized {
    val points = new vtk.vtkPoints()
    val bb = source.source.boundingBox
    points.InsertNextPoint(bb.xMin, bb.yMin, bb.zMin)
    points.InsertNextPoint(bb.xMax, bb.yMax, bb.zMax)

    val poly = new vtk.vtkPolyData()
    poly.SetPoints(points)

    val outline = new vtk.vtkOutlineFilter()
    outline.SetInputData(poly)
    mapper.SetInputConnection(outline.GetOutputPort())
    mapper.Modified()
    if (withEvent) {
      publish(VtkContext.RenderRequest(this))
    }
  }

  override def onDestroy() = this.synchronized {
    deafTo(scene)
    super.onDestroy()
  }

  override lazy val currentBoundingBox = BoundingBox.None
}