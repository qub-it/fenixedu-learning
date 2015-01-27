package org.fenixedu.learning.task;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.learning.domain.degree.DegreeSite;
import org.fenixedu.learning.domain.degree.DegreeSiteListener;

public class LoadAllDegreeSites extends CustomTask {

	@Override
	public void runTask() throws Exception {
		for (Degree degree : Bennu.getInstance().getDegreesSet()) {
			/*
			 * Since all Degree instances were created by script, no signal
			 * was raised, and therefore no LMS structures were created.
			 */
			if (degree.getSite() == null) {
				DegreeSite s = DegreeSiteListener.create(degree);
				s.setFolder(Bennu.getInstance().getCmsFolderSet().iterator().next());
				System.out.println(degree.getPresentationName());
			}
		}
	}

}
