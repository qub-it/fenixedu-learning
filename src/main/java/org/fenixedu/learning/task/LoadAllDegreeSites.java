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
                /*This lines are now inside the constructor*/
                //s.setFolder(folderForPath(PortalConfiguration.getInstance().getMenu(), "degrees"));
                //s.setSlug(on("-").join(degree.getSigla(), degree.getExternalId()));
                System.out.println(degree.getPresentationName());
            }
        }
    }

//	private CMSFolder folderForPath(MenuContainer parent, String path) {
//
//        return parent.getOrderedChild().stream().filter(item -> item.getPath().equals(path))
//                .map(item -> item.getAsMenuFunctionality().getCmsFolder()).findFirst().orElseThrow(() -> new RuntimeException("no.degree.site.folder.was.found"));
//    }

}
