package scalismo.ui.vtk

import scalismo.ui.ScalarMeshFieldView.ScalarMeshFieldRenderable3D
import scalismo.utils.MeshConversion

class ScalarMeshFieldActor(renderable: ScalarMeshFieldRenderable3D) extends SinglePolyDataActor with ActorOpacity {

  override lazy val opacity = renderable.opacity

  val meshData = renderable.source.source
  val vtkpd = MeshConversion.scalarMeshFieldToVtkPolyData(meshData)

  mapper.SetInputData(vtkpd)
  mapper.ScalarVisibilityOn()
  mapper.SetScalarRange(meshData.values.min, meshData.values.max)

  setGeometry()

  def setGeometry() = this.synchronized {
    vtkpd.Modified()
    mapper.Modified()
    publishEdt(VtkContext.RenderRequest(this))
  }

  override def onDestroy() = this.synchronized {
    super.onDestroy()
  }
}