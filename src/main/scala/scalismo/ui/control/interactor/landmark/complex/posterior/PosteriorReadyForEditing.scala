/*
 * Copyright (C) 2016  University of Basel, Graphics and Vision Research Group 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scalismo.ui.control.interactor.landmark.complex.posterior

import scalismo.ui.control.interactor.landmark.complex.ComplexLandmarkingInteractor.{ Delegate, StateTransition }
import scalismo.ui.control.interactor.landmark.complex.{ ComplexLandmarkingInteractor, ReadyForEditing }
import scalismo.ui.model.LandmarkNode

object PosteriorReadyForEditing {
  def enter[InteractorType <: ComplexLandmarkingInteractor[InteractorType], DelegateType <: Delegate[InteractorType]]: StateTransition[InteractorType, DelegateType] = new StateTransition[InteractorType, DelegateType] {
    override def apply()(implicit parent: InteractorType) = new PosteriorReadyForEditing()
  }
}

class PosteriorReadyForEditing[InteractorType <: ComplexLandmarkingInteractor[InteractorType]](implicit parent: InteractorType) extends ReadyForEditing[InteractorType] {

  override def transitionToReadyForCreating(): Unit = {
    parent.transitionTo(PosteriorReadyForCreating.enter)
  }

  def transitionToEditing(modelLm: LandmarkNode, targetLm: LandmarkNode): Unit = {
    parent.transitionTo(PosteriorEditing.enter(modelLm, targetLm))
  }
}

