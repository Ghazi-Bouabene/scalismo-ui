package org.statismo.stk.ui.vtk

import java.awt.Color
import java.awt.Graphics

import org.statismo.stk.ui.Viewport
import org.statismo.stk.ui.Workspace

import vtk.vtkCanvas
import vtk.vtkInteractorStyleTrackballCamera
import scala.swing.Swing

class VtkCanvas(workspace: Workspace, viewport: Viewport) extends vtkCanvas {
  lazy val interactor = new VtkRenderWindowInteractor(workspace, viewport)
  iren = interactor

  iren.SetRenderWindow(rw)
  iren.SetSize(this.getSize.width, this.getSize.height)
  iren.ConfigureEvent()
  iren.SetInteractorStyle(new vtkInteractorStyleTrackballCamera)

  private var isEmpty = true

  override def Render() = this.synchronized {
    isEmpty = false
    Swing.onEDT{
      super.Render()
    }
  }

  def setAsEmpty() = this.synchronized {
    isEmpty = true
    repaint()
  }

  override def paint(g: Graphics) = this.synchronized {
    if (isEmpty) {
      g.setColor(Color.BLACK)
      g.fillRect(0, 0, getWidth, getHeight)
    } else {
      super.paint(g)
    }
  }

  override def finalize() = {
    println("Finalizing")
    super.finalize()
  }
}