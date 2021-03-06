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

import static java.util.stream.Collectors.toList;
import static org.fenixedu.academic.domain.ExecutionCourse.EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR;
import static pt.ist.fenixframework.FenixFramework.getDomainObject;

import java.util.HashMap;
import java.util.Map;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;

import com.google.common.collect.Maps;

/**
 * Created by borgez on 10/15/14.
 */
@ComponentType(name = "Curricular Course", description = "Curricular course info")
public class CurricularCourseComponent extends DegreeSiteComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        CurricularCourse curricularCourse = getDomainObject(globalContext.getRequestContext()[1]);
        ExecutionInterval period = getExecutionSemester(curricularCourse, globalContext.getRequestContext());
        globalContext.put("curricularCourse", createWrap(curricularCourse, period));
    }

    private ExecutionInterval getExecutionSemester(CurricularCourse curricularCourse, String[] request) {
        return request.length > 2 ? getDomainObject(request[2]) : curricularCourse.getParentDegreeCurricularPlan()
                .getMostRecentExecutionYear().getLastExecutionPeriod();
    }

    private HashMap<String, Object> createWrap(CurricularCourse curricularCourse, ExecutionInterval period) {
        HashMap<String, Object> wrap = Maps.newHashMap();
        wrap.put("period", period);
        wrap.put("name", curricularCourse.getNameI18N(period));
        wrap.put("degreeCurricularPlanName", curricularCourse.getDegreeCurricularPlan().getPresentationName());
        wrap.put("acronym", curricularCourse.getAcronym(period));
        wrap.put("isOptional", curricularCourse.isOptionalCurricularCourse());
        wrap.put(
                "executionCourses",
                curricularCourse.getAssociatedExecutionCoursesSet().stream()
                        .sorted(EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR.reversed()).map(ec -> createWrap(ec))
                        .collect(toList()));
        wrap.put("parentContexts", curricularCourse.getParentContextsByExecutionYear(period.getExecutionYear()));
        wrap.put("weight", curricularCourse.getWeight(period));
        wrap.put("objectives", curricularCourse.getObjectivesI18N(period));
        wrap.put("program", curricularCourse.getProgramI18N(period));
        wrap.put("evaluationMethod", curricularCourse.getEvaluationMethodI18N(period));
        return wrap;
    }

    private Map<String, Object> createWrap(ExecutionCourse executionCourse) {
        Map<String, Object> wrap = Maps.newHashMap();
        wrap.put("name", executionCourse.getNameI18N());
        wrap.put("executionYear", executionCourse.getExecutionYear().getYear());
        wrap.put("executionPeriod", executionCourse.getExecutionPeriod().getName());
        wrap.put("url", executionCourse.getSite() != null ? executionCourse.getSite().getFullUrl() : "#");
        return wrap;
    }
}
