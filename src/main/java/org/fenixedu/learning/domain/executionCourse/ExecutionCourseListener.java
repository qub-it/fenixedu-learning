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

import static com.google.common.base.Joiner.on;

import java.util.Objects;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.cms.domain.Role;
import org.fenixedu.cms.domain.RoleTemplate;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionCourseListener {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionCourseListener.class);

    public static Site create(ExecutionCourse executionCourse) {
        Site newSite = ExecutionCourseSiteBuilder.getInstance().create(executionCourse.getNameI18N(),
                getObjectives(executionCourse).orElseGet(() -> executionCourse.getNameI18N()),
                formatSlugForExecutionCourse(executionCourse));

        executionCourse.setSite(newSite);

        RoleTemplate defaultTemplate = newSite.getDefaultRoleTemplate();
        if (defaultTemplate != null) {
            Role teacherRole = new Role(defaultTemplate, executionCourse.getSite());

            Group group = teacherRole.getGroup();
            for (Professorship pr : executionCourse.getProfessorshipsSet()) {
                User user = pr.getPerson().getUser();
                if (pr.getPermissions().getSections() && !group.isMember(user)) {
                    group = group.grant(user);
                } else if (group.isMember(user)) {
                    group = group.revoke(user);
                }
            }
            teacherRole.setGroup(group);
        } else {
            throw new DomainException("no.default.role");
        }

        logger.info("Created site for execution course " + executionCourse.getSigla());
        return newSite;
    }

    private static Optional<LocalizedString> getObjectives(ExecutionCourse executionCourse) {
        return executionCourse.getCompetenceCourses().stream()
                .map(competenceCourse -> competenceCourse.getObjectivesI18N(executionCourse.getExecutionPeriod()))
                .filter(Objects::nonNull).findFirst();
    }

    private static String formatSlugForExecutionCourse(ExecutionCourse executionCourse) {
        return on("-").join(executionCourse.getSigla(), executionCourse.getExternalId());
    }

    public static void updateSiteSlug(ExecutionCourse instance) {
        instance.getSite().setSlug(formatSlugForExecutionCourse(instance));
        instance.setSiteUrl(instance.getSite().getFullUrl());
    }

}
