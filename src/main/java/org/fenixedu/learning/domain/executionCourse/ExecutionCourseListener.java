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
import static org.fenixedu.cms.domain.component.Component.forType;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.cms.domain.CMSTheme;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ListCategoryPosts;
import org.fenixedu.cms.domain.component.ViewPost;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.learning.domain.executionCourse.components.BibliographicReferencesComponent;
import org.fenixedu.learning.domain.executionCourse.components.EvaluationMethodsComponent;
import org.fenixedu.learning.domain.executionCourse.components.EvaluationsComponent;
import org.fenixedu.learning.domain.executionCourse.components.ExecutionCourseComponent;
import org.fenixedu.learning.domain.executionCourse.components.GroupsComponent;
import org.fenixedu.learning.domain.executionCourse.components.InitialPageComponent;
import org.fenixedu.learning.domain.executionCourse.components.LessonPlanComponent;
import org.fenixedu.learning.domain.executionCourse.components.MarksComponent;
import org.fenixedu.learning.domain.executionCourse.components.ObjectivesComponent;
import org.fenixedu.learning.domain.executionCourse.components.ScheduleComponent;

public class ExecutionCourseListener {
    public static final String BUNDLE = "resources.FenixEduLearningResources";

    public static final LocalizedString ANNOUNCEMENTS_TITLE = getLocalizedString(BUNDLE, "label.announcements");
    public static final LocalizedString VIEW_POST_TITLE = getLocalizedString(BUNDLE, "label.viewPost");
    private static final LocalizedString INITIAL_PAGE_TITLE = getLocalizedString(BUNDLE, "label.initialPage");
    private static final LocalizedString GROUPS_TITLE = getLocalizedString(BUNDLE, "label.groups");
    private static final LocalizedString EVALUATIONS_TITLE = getLocalizedString(BUNDLE, "label.evaluations");
    private static final LocalizedString REFERENCES_TITLE = getLocalizedString(BUNDLE, "label.bibliographicReferences");
    private static final LocalizedString SCHEDULE_TITLE = getLocalizedString(BUNDLE, "label.schedule");
    private static final LocalizedString EVALUATION_METHOD_TITLE = getLocalizedString(BUNDLE, "label.evaluationMethods");
    private static final LocalizedString OBJECTIVES_TITLE = getLocalizedString(BUNDLE, "label.objectives");
    private static final LocalizedString MARKS_TITLE = getLocalizedString(BUNDLE, "label.marks");
    private static final LocalizedString LESSON_PLAN_TITLE = getLocalizedString(BUNDLE, "label.lessonsPlanings");
    private static final LocalizedString PROGRAM_TITLE = getLocalizedString(BUNDLE, "label.program");
    private static final LocalizedString INQUIRIES_RESULTS_TITLE = getLocalizedString(BUNDLE, "label.inquiriesResults");
    private static final LocalizedString SUMMARIES_TITLE = getLocalizedString(BUNDLE, "label.summaries");
    private static final LocalizedString SHIFTS_TITLE = getLocalizedString(BUNDLE, "label.shifts");
    public static final LocalizedString MENU_TITLE = getLocalizedString("resources.FenixEduLearningResources", "label.menu");

    public static ExecutionCourseSite create(ExecutionCourse executionCourse) {
        final ExecutionCourseSite newSite = new ExecutionCourseSite(executionCourse);
        executionCourse.setSite(newSite);
        final Menu menu = new Menu(newSite);
        menu.setName(MENU_TITLE);
        newSite.setTheme(CMSTheme.forType("fenixedu-learning-theme"));
        createDefaultContents(newSite, menu, Authenticate.getUser());
        return newSite;
    }

    public static void createDefaultContents(Site site, Menu menu, User author) {

        Category summariesCategory = site.getOrCreateCategoryForSlug("summary", SUMMARIES_TITLE);
        Category announcementsCategory = site.getOrCreateCategoryForSlug("announcement", ANNOUNCEMENTS_TITLE);

        ListCategoryPosts summariesComponent = new ListCategoryPosts(summariesCategory);
        ListCategoryPosts announcementsComponent = new ListCategoryPosts(announcementsCategory);

        Component referencesComponent = forType(BibliographicReferencesComponent.class);
        Component evaluationMethodsComponent = forType(EvaluationMethodsComponent.class);
        // TODO: recreate this somehow
        //Component inquiriesResultsComponent = forType(InquiriesResultsComponent.class);
        Component homeComponent = forType(InitialPageComponent.class);

        Page initialPage =
                Page.create(site, menu, null, INITIAL_PAGE_TITLE, true, "firstPage", author, homeComponent,
                        announcementsComponent);
        Page.create(site, menu, null, GROUPS_TITLE, true, "groupings", author, forType(GroupsComponent.class));
        Page.create(site, menu, null, EVALUATIONS_TITLE, true, "evaluations", author, forType(EvaluationsComponent.class));
        Page.create(site, menu, null, REFERENCES_TITLE, true, "bibliographicReferences", author, referencesComponent);
        Page.create(site, menu, null, SCHEDULE_TITLE, true, "calendarEvents", author, forType(ScheduleComponent.class));
        Page.create(site, menu, null, EVALUATION_METHOD_TITLE, true, "evaluationMethods", author, evaluationMethodsComponent);
        Page.create(site, menu, null, OBJECTIVES_TITLE, true, "objectives", author, forType(ObjectivesComponent.class));
        Page.create(site, menu, null, LESSON_PLAN_TITLE, true, "lessonPlan", author, forType(LessonPlanComponent.class));
        Page.create(site, menu, null, PROGRAM_TITLE, true, "program", author, forType(ObjectivesComponent.class));
        //Page.create(newSite, menu, null, INQUIRIES_RESULTS_TITLE, true, "inqueriesResults", user, inquiriesResultsComponent, menuComponent);
        Page.create(site, menu, null, SHIFTS_TITLE, true, "shifts", author, forType(ExecutionCourseComponent.class));
        Page.create(site, menu, null, ANNOUNCEMENTS_TITLE, true, "category", author, announcementsComponent);
        Page.create(site, menu, null, SUMMARIES_TITLE, true, "category", author, summariesComponent);
        Page.create(site, menu, null, MARKS_TITLE, true, "marks", author, forType(MarksComponent.class));
        Page.create(site, null, null, VIEW_POST_TITLE, true, "view", author, forType(ViewPost.class));
        //TODO content search
        site.setInitialPage(initialPage);

    }
}
