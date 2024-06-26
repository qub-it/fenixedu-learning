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
package org.fenixedu.learning.domain.degree.components;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Coordinator;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeInfo;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;
import org.fenixedu.learning.domain.DegreeCurricularPlanServices;
import org.fenixedu.spaces.domain.Space;

import pt.ist.fenixframework.FenixFramework;

@ComponentType(name = "degreeDescription", description = "Description of a Degree")
public class DescriptionComponent extends DegreeSiteComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext global) {
        Degree degree = degree(page);

        global.put("degreeName", degree.getPresentationName());

        ExecutionYear targetExecutionYear = getTargetExecutionYear(global, degree);
        global.put("year", targetExecutionYear.getYear());

        final String campusName =
                Optional.ofNullable(degree).map(Degree::getUnit).map(Unit::getCampus).map(Space::getName).orElse("");
        global.put("campi", campusName);

        Collection<Teacher> responsibleCoordinatorsTeachers = getResponsibleCoordinatorsTeachers(degree, targetExecutionYear);
        if (responsibleCoordinatorsTeachers.isEmpty()) {
            responsibleCoordinatorsTeachers = getCurrentResponsibleCoordinatorsTeachers(degree);
        }
        global.put("coordinators", responsibleCoordinatorsTeachers);

        DegreeInfo degreeInfo = degree.getDegreeInfoFor(targetExecutionYear);
        if (degreeInfo == null) {
            degreeInfo = degree.getMostRecentDegreeInfo(targetExecutionYear.getAcademicInterval());
        }
        global.put("degreeInfo", degreeInfo);

    }

    private static Collection<Teacher> getResponsibleCoordinatorsTeachers(final Degree degree,
            final ExecutionYear executionYear) {
        return Coordinator.findCoordinators(degree, executionYear, true).map(c -> c.getPerson().getTeacher())
                .filter(Objects::nonNull).distinct().sorted(Teacher.TEACHER_COMPARATOR_BY_CATEGORY_AND_NUMBER)
                .collect(Collectors.toUnmodifiableList());
    }

    private static Collection<Teacher> getCurrentResponsibleCoordinatorsTeachers(final Degree degree) {
        return Coordinator.findLastCoordinators(degree, true).map(c -> c.getPerson().getTeacher()).filter(Objects::nonNull)
                .distinct().sorted(Teacher.TEACHER_COMPARATOR_BY_CATEGORY_AND_NUMBER).collect(Collectors.toUnmodifiableList());
    }

    private ExecutionYear getTargetExecutionYear(TemplateContext global, Degree degree) {
        String executionDegreeId = global.getParameter("executionDegreeID");
        if (executionDegreeId != null) {

            ExecutionDegree executionDegree = FenixFramework.getDomainObject(executionDegreeId);
            if (executionDegree == null || !executionDegree.getDegreeCurricularPlan().getDegree().equals(degree)) {
                throw new RuntimeException("Unknown Execution Degree identifier.");
            }

            return executionDegree.getExecutionYear();

        } else {

            // qubExtension, select latest (provided that it is clear on the Html)
            final DegreeInfo latest =
                    degree.getDegreeInfosSet().stream().max(DegreeInfo.COMPARATOR_BY_EXECUTION_YEAR).orElse(null);
            if (latest != null) {
                return latest.getExecutionYear();

            } else {

                final ExecutionYear current = ExecutionYear.readCurrentExecutionYear();
                final Set<ExecutionYear> years = DegreeCurricularPlanServices.getDegreeCurricularPlansExecutionYears(degree);

                // qubExtension, refactored
                if (years.isEmpty() || years.contains(current)) {
                    return current;

                } else {
                    final ExecutionYear first = years.stream().min(ExecutionYear::compareTo).orElse(null);
                    final ExecutionYear last = years.stream().max(ExecutionYear::compareTo).orElse(null);
                    return current.isBefore(first) ? first : last;
                }
            }
        }
    }
}
