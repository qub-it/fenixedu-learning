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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.MoreObjects;

public class CompetenceCourseBean {
    private final CompetenceCourse competenceCourse;
    private final ExecutionInterval executionInterval;
    private final Set<CurricularCourse> curricularCourses;
    private final LocalizedString name;
    private final LocalizedString objectives;
    private final LocalizedString program;

    public CompetenceCourseBean(CompetenceCourse competenceCourse, Set<CurricularCourse> curricularCourses,
            ExecutionInterval executionInterval) {
        this.competenceCourse = competenceCourse;
        this.executionInterval = executionInterval;
        this.curricularCourses = curricularCourses;
        this.name = competenceCourse.getNameI18N(executionInterval);
        this.objectives = competenceCourse.getObjectivesI18N(executionInterval);
        this.program = competenceCourse.getProgramI18N(executionInterval);
    }

    public CompetenceCourse getCompetenceCourse() {
        return competenceCourse;
    }

    @Deprecated
    public ExecutionInterval getExecutionSemester() {
        return getExecutionInterval();
    }

    public ExecutionInterval getExecutionInterval() {
        return executionInterval;
    }

    public Set<CurricularCourse> getCurricularCourses() {
        return curricularCourses;
    }

    public LocalizedString getName() {
        return name;
    }

    public LocalizedString getObjectives() {
        return objectives;
    }

    public static List<CompetenceCourseBean> approvedCompetenceCourses(ExecutionCourse executionCourse) {
        return executionCourse.getCurricularCoursesIndexedByCompetenceCourse().entrySet().stream()
                .filter(entry -> entry.getKey().isApproved())
                .map(entry -> new CompetenceCourseBean(entry.getKey(), entry.getValue(), executionCourse.getExecutionPeriod()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", this.name).add("objectives", this.objectives)
                .add("executionInterval", executionInterval).add("curricularCourses", curricularCourses).toString();
    }

    public LocalizedString getProgram() {
        return program;
    }
}
