/**
 * Copyright © 2015 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Learning.
 *
 * FenixEdu Learning is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Learning is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Learning.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.learning.domain.executionCourse;

import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;

import com.google.common.base.Strings;

public class SummaryListener {

    public static final String BUNDLE = "resources.FenixEduLearningResources";
    public static final LocalizedString SUMMARIES_TITLE = getLocalizedString(BUNDLE, "label.summaries");
    public static final String SUMMARIES_CATEGORY = "summary";

    public static void updatePost(Post post, Summary summary) {
        ExecutionCourseSite site = summary.getExecutionCourse().getSite();

        summary.setPost(post);
        post.setSlug("summary-" + summary.getOid());
        post.setName(summary.getTitle().toLocalizedString());

        post.setBody(summary.getSummaryText().toLocalizedString());
        post.setCreationDate(summary.getSummaryDateTime());

        post.addCategories(site.getOrCreateCategoryForSlug(SUMMARIES_CATEGORY, SUMMARIES_TITLE));
    }

}
