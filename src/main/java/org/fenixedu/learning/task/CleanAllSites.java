package org.fenixedu.learning.task;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CleanAllSites extends CustomTask {

	@Override
	public void runTask() throws Exception {
		for (ExecutionCourse ec : Bennu.getInstance().getExecutionCoursesSet()) {
			if (ec.getSite() != null) {
				ec.getSite().delete();
			}
		}
		for (Degree degree : Bennu.getInstance().getDegreesSet()) {
			if (degree.getSite() != null) {
				degree.getSite().delete();
			}
		}
	}

}
