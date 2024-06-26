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
package org.fenixedu.learning.domain.executionCourse.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.LessonPlanning;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;

@ComponentType(name = "LessonsPlanning", description = "Lessons planing for an Execution Course")
public class LessonPlanComponent extends BaseExecutionCourseComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        ExecutionCourse executionCourse = page.getSite().getExecutionCourse();
        if (executionCourse.getLessonPlanningAvailable()) {
            Map<?, List<LessonPlanning>> lessonPlanningsMap = new HashMap<>();
//            for (ShiftType shiftType : executionCourse.getShiftTypes()) {
//                List<LessonPlanning> lessonPlanningsOrderedByOrder = LessonPlanning.findOrdered(executionCourse, shiftType);
//                if (!lessonPlanningsOrderedByOrder.isEmpty()) {
//                    lessonPlanningsMap.put(shiftType, lessonPlanningsOrderedByOrder);
//                }
//            }
            globalContext.put("lessonPlanningsMap", lessonPlanningsMap);
        }
    }

}
