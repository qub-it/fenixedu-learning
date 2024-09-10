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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.BibliographicReference;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;

@ComponentType(name = "bibliographicReferences", description = "Bibliographic References for an Execution Course")
public class BibliographicReferencesComponent extends BaseExecutionCourseComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        ExecutionCourse executionCourse = page.getSite().getExecutionCourse();

        Map<Boolean, List<BibliographicReference>> bibliographicReferencesByOptional = bibliographicReferences(executionCourse);

        globalContext.put("executionCourse", executionCourse);
        globalContext.put("mainReferences", bibliographicReferencesByOptional.get(false));
        globalContext.put("secondaryReferences", bibliographicReferencesByOptional.get(true));
        globalContext.put("optionalReferences", Collections.emptyList());
        globalContext.put("nonOptionalReferences", Collections.emptyList());
    }

    private Map<Boolean, List<BibliographicReference>> bibliographicReferences(ExecutionCourse executionCourse) {

        return executionCourse.getCompetenceCourses().stream().flatMap(cc -> cc.findBibliographies())
                .sorted(BibliographicReference.COMPARATOR_BY_ORDER).collect(Collectors.partitioningBy(br -> br.isOptional()));
    }

}
