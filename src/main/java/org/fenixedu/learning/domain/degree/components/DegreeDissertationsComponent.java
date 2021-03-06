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

import static org.fenixedu.academic.domain.Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;

import com.google.common.collect.ImmutableSet;

@Deprecated
@ComponentType(name = "degreeDissertations", description = "Dissertations information for a Degree")
public class DegreeDissertationsComponent extends DegreeSiteComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        Collection<Degree> degrees = ImmutableSet.of(degree(page));

        globalContext.put("unit", degree(page).getUnit());
        globalContext.put("thesesByYear", new HashMap<>());
        globalContext.put("years", Stream.empty());
        globalContext.put("degrees", degrees.stream().sorted(COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID));
        globalContext.put("states", new HashMap<>());
        Page thesisPage = DegreeSiteComponent.pageForComponent(page.getSite(), ThesisComponent.class).get();
        globalContext.put("dissertationUrl", thesisPage.getAddress());
    }

}
