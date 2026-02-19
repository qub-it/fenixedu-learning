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

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;
import org.fenixedu.learning.domain.DegreeCurricularPlanServices;

import com.google.common.collect.Maps;

/**
 * Created by borgez on 10/8/14.
 */
@ComponentType(name = "Degree execution courses", description = "execution courses for a degree")
public class DegreeExecutionCoursesComponent extends DegreeSiteComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        Degree degree = degree(page);
        globalContext.put("executionCoursesBySemesterAndCurricularYear", executionCourses(degree));

        // qubExtension
        globalContext.put("executionYears", DegreeCurricularPlanServices.getDegreeCurricularPlansExecutionYears(degree).stream()
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
    }

    public SortedMap<ExecutionInterval, SortedMap<Integer, SortedSet<ExecutionCourse>>> executionCourses(final Degree degree) {
        TreeMap<ExecutionInterval, SortedMap<Integer, SortedSet<ExecutionCourse>>> result =
                Maps.newTreeMap(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE);

        final ExecutionInterval current = ExecutionInterval.findFirstCurrentChild(null);
        final ExecutionInterval previous = current.getPrevious();
        final ExecutionInterval next = current.getNext();
        final boolean hasNext = next != null && next.getExecutionYear().isCurrent() && next.getState().equals(PeriodState.OPEN);
        final ExecutionInterval selected = hasNext ? next : previous;

        result.put(selected, executionCourses(degree, selected));
        result.put(current, executionCourses(degree, current));
        return result;
    }

    public SortedMap<Integer, SortedSet<ExecutionCourse>> executionCourses(Degree degree, ExecutionInterval executionInterval) {
        SortedMap<Integer, SortedSet<ExecutionCourse>> courses = new TreeMap<>();

        if (executionInterval != null) {
            DegreeCurricularPlanServices
                    .getDegreeCurricularPlansForYear(degree, Optional.of(executionInterval.getExecutionYear()))
                    .forEach(plan -> addExecutionCourses(plan.getRoot(), courses, executionInterval));
        }

        return courses;
    }

    private void addExecutionCourses(final CourseGroup courseGroup, Map<Integer, SortedSet<ExecutionCourse>> courses,
            final ExecutionInterval... executionPeriods) {
        for (final Context context : courseGroup.getChildContextsSet()) {
            for (final ExecutionInterval executionSemester : executionPeriods) {
                if (context.isValid(executionSemester) && !isUnavailableOnSitesAndAPIsRule(context, executionSemester)) {
                    final DegreeModule degreeModule = context.getChildDegreeModule();
                    if (degreeModule.isLeaf()) {
                        final CurricularCourse curricularCourse = (CurricularCourse) degreeModule;
                        for (final ExecutionCourse executionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
                            if (executionCourse.getExecutionPeriod() == executionSemester) {
                                courses.computeIfAbsent(context.getCurricularYear(),
                                        i -> new TreeSet<ExecutionCourse>(ExecutionCourse.EXECUTION_COURSE_NAME_COMPARATOR))
                                        .add(executionCourse);
                            }
                        }
                    } else {
                        final CourseGroup childCourseGroup = (CourseGroup) degreeModule;
                        addExecutionCourses(childCourseGroup, courses, executionPeriods);
                    }
                }
            }
        }
    }

    private static boolean isUnavailableOnSitesAndAPIsRule(Context context, ExecutionInterval executionInterval) {
        return context.getChildDegreeModule().getCurricularRules(context, executionInterval).stream()
                .anyMatch(r -> r.getClass().getSimpleName().equals("UnavailableOnSitesAndAPIsRule"));
    }
}
