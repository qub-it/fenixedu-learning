package org.fenixedu.learning.task;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;

public class LoadAllCourseSites extends CustomTask {

	@Override
	public void runTask() throws Exception {
		for (ExecutionCourse ec : Bennu.getInstance().getExecutionCoursesSet()) {
			/*
			 * Since all ExecutionCourse instances were created by script, no signal
			 * was raised, and therefore no LMS structures were created.
			 */
			if (ec.getSite() == null) {
				Signal.emit(ExecutionCourse.CREATED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(ec));
			}
		}
	}

}
