package org.statismo.stk.ui.view.swing

import scala.swing.SplitPane
import scala.swing.Orientation
import scala.swing.BorderPanel
import scala.swing.BorderPanel.Position._
import org.statismo.stk.ui.Workspace

class PropertiesPanel(val workspace: Workspace) extends BorderPanel {
	val scene = new ScenePropertiesPanel(workspace);
	val details = new SceneObjectPropertiesPanel(workspace);
	
	val split = new SplitPane(Orientation.Horizontal, scene, details)
	layout(split) = Center
}