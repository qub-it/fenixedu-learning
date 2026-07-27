package org.fenixedu.learning.task;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.CMSTemplate;
import org.fenixedu.cms.domain.CMSTheme;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ListCategoryPosts;
import org.fenixedu.cms.domain.component.StaticPost;
import org.fenixedu.commons.i18n.LocalizedString;

import com.qubit.solution.fenixedu.module.cmsui.domain.CmsSiteType;
import com.qubit.solution.fenixedu.module.cmsui.domain.component.CmsEmbeddedPagesRenderComponent;
import com.qubit.solution.fenixedu.module.cmsui.domain.component.CmsWidgetContainersRenderComponent;
import com.qubit.solution.fenixedu.module.cmsui.domain.listeners.DegreeSiteListener;
import com.qubit.solution.fenixedu.module.cmsui.domain.listeners.ExecutionCourseSiteListener;
import com.qubit.solution.fenixedu.module.cmsui.services.SiteServices;
import com.qubit.terra.cms.domain.widget.CmsWidget;
import com.qubit.terra.cms.domain.widget.CmsWidgetType;
import com.qubit.terra.cms.domain.widget.layout.CmsWidgetArea;
import com.qubit.terra.cms.domain.widget.layout.CmsWidgetContainer;
import com.qubit.terra.cms.domain.widget.property.CmsWidgetProperty;

/**
 * CustomTask that migrates old-structure Degree and Execution Course CMS sites
 * (created via SiteBuilder) to the new CmsSiteType + widget-based structure.
 *
 * Old-structure sites are identified by having {@code siteType == null}.
 * Old pages are identified by their template type (e.g., "firstPage", "evaluations")
 * which differs from the new templates ("singleAreaPage", "View Post Embebbed").
 *
 * This task is idempotent: running it multiple times will not create duplicate
 * widgets or containers.
 */
public class MigrateOldSitesToNewCmsStructure extends CustomTask {

    private static final String ANNOUNCEMENT_CATEGORY_SLUG = "announcement";
    private static final String SUMMARY_CATEGORY_SLUG = "summary";

    // Fully qualified data provider class names (cross-module references)
    private static final String DEGREE_INFO_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.degree.DegreeInformationProvider";
    private static final String DEGREE_CURRICULUM_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.degree.DegreeCurriculumProvider";
    private static final String DEGREE_EC_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.degree.DegreeExecutionCoursesProvider";
    private static final String DEGREE_CLASSES_SCHEDULE_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.degree.DegreeClassesScheduleProvider";
    private static final String DEGREE_EVALUATIONS_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.degree.DegreeEvaluationsProvider";
    private static final String EC_INFO_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.executionCourse.ExecutionCourseInformationProvider";
    private static final String EC_COMM_MESSAGES_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.executionCourse.ExecutionCourseCommunicationMessagesDataProvider";
    private static final String EC_SCHEDULE_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.executionCourse.ExecutionCourseScheduleProvider";
    private static final String EC_PLANNING_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.executionCourse.ViewExecutionCoursePlanning";
    private static final String POSTS_DATA_PROVIDER =
            "com.qubit.solution.fenixedu.module.cmsui.domain.widget.dataprovider.PostsDataProvider";

    private CMSTemplate singleAreaPageTemplate;
    private CMSTemplate viewPostEmbeddedTemplate;

    @Override
    public void runTask() throws Exception {
        final User user = Authenticate.getUser();
        if (user == null) {
            taskLog("WARNING: No authenticated user. Setting system user for migration.");
            final Optional<User> systemUser =
                    org.fenixedu.bennu.core.domain.Bennu.getInstance().getUserSet().stream().findFirst();
            if (systemUser.isPresent()) {
                Authenticate.mock(systemUser.get(), "MigrateOldSitesToNewCmsStructure");
            } else {
                taskLog("ERROR: No users found in the system. Cannot proceed with migration.");
                return;
            }
        }

        taskLog("Starting migration of old CMS sites to new widget-based structure.");

        migrateDegreeSites();

        migrateExecutionCourseSites();

        taskLog("Migration completed.");
    }

    private void migrateDegreeSites() {
        final CmsSiteType degreeSiteType = CmsSiteType.findByCode(DegreeSiteListener.DEGREE_SITE_TYPE);
        if (degreeSiteType == null) {
            taskLog("WARNING: DEGREE_SITE CmsSiteType not found. Degree site type may not be initialized yet. Skipping.");
            return;
        }

        final CMSTemplate[] templates = findRequiredTemplates(degreeSiteType);
        if (templates == null) {
            taskLog("WARNING: Required templates not found for DEGREE_SITE. Skipping degree migration.");
            return;
        }
        singleAreaPageTemplate = templates[0];
        viewPostEmbeddedTemplate = templates[1];

        int migrated = 0;
        int skipped = 0;
        for (Degree degree : Bennu.getInstance().getDegreesSet()) {
            final Site site = degree.getSite();
            if (site == null) {
                continue;
            }
            if (site.getSiteType() != null) {
                skipped++;
                continue;
            }

            try {
                migrateDegreeSite(site, degreeSiteType);
                migrated++;
            } catch (Exception e) {
                taskLog("ERROR migrating degree site '%s': %s", site.getSlug(), e.getMessage());
            }
        }
        taskLog("Degree sites: %d migrated, %d skipped (already new structure).", migrated, skipped);
    }

    private void migrateExecutionCourseSites() {
        final CmsSiteType ecSiteType = CmsSiteType.findByCode(ExecutionCourseSiteListener.EXECUTION_COURSE_SITE_TYPE);
        if (ecSiteType == null) {
            taskLog("WARNING: EXECUTION_COURSE_SITE CmsSiteType not found. "
                    + "Execution course site type may not be initialized yet. Skipping.");
            return;
        }

        final CMSTemplate[] templates = findRequiredTemplates(ecSiteType);
        if (templates == null) {
            taskLog("WARNING: Required templates not found for EXECUTION_COURSE_SITE. Skipping EC migration.");
            return;
        }
        singleAreaPageTemplate = templates[0];
        viewPostEmbeddedTemplate = templates[1];

        int migrated = 0;
        int skipped = 0;
        for (ExecutionCourse ec : Bennu.getInstance().getExecutionCoursesSet()) {
            final Site site = ec.getSite();
            if (site == null) {
                continue;
            }
            if (site.getSiteType() != null) {
                skipped++;
                continue;
            }

            try {
                migrateExecutionCourseSite(site, ecSiteType);
                migrated++;
            } catch (Exception e) {
                taskLog("ERROR migrating EC site '%s': %s", site.getSlug(), e.getMessage());
            }
        }
        taskLog("Execution Course sites: %d migrated, %d skipped (already new structure).", migrated, skipped);
    }

    private CMSTemplate[] findRequiredTemplates(CmsSiteType siteType) {
        final CMSTheme theme = siteType.getCmsSiteTypeTheme();
        if (theme == null) {
            return null;
        }

        final Optional<CMSTemplate> singleArea =
                theme.getAllTemplates().stream().filter(t -> "singleAreaPage".equals(t.getName())).findFirst();
        final Optional<CMSTemplate> viewEmbedded =
                theme.getAllTemplates().stream().filter(t -> "View Post Embebbed".equals(t.getName())).findFirst();

        if (!singleArea.isPresent() || !viewEmbedded.isPresent()) {
            return null;
        }

        return new CMSTemplate[] { singleArea.get(), viewEmbedded.get() };
    }

    private boolean needsMigration(Page page) {
        final String templateType = page.getTemplateType();
        if (templateType == null) {
            return false;
        }

        if ("singleAreaPage".equals(templateType) || "View Post Embebbed".equals(templateType)) {
            return false;
        }

        if (hasComponentOfType(page, CmsWidgetContainersRenderComponent.class)) {
            return false;
        }

        return true;
    }

    private void migrateDegreeSite(Site site, CmsSiteType siteType) {
        taskLog("Migrating degree site: %s", site.getSlug());

        site.setSiteType(siteType);
        site.setTheme(siteType.getCmsSiteTypeTheme());

        ensureCategoryExists(site, ANNOUNCEMENT_CATEGORY_SLUG,
                new LocalizedString.Builder()
                        .with(Locale.getDefault(), "Anúncios")
                        .with(Locale.ENGLISH, "Announcements").build());

        for (Page page : site.getPagesSet()) {
            if (!needsMigration(page)) {
                continue;
            }

            final String templateType = page.getTemplateType();
            taskLog("  Migrating page '%s' (template: %s)", page.getName().getContent(), templateType);

            switch (templateType) {
                case "degreeDescription":
                    migrateToWidgetPage(page, site, DEGREE_INFO_PROVIDER, "View Degree Description");
                    break;
                case "category":
                    migrateCategoryPageDegree(page, site);
                    break;
                case "degreeCurriculum":
                    migrateToWidgetPage(page, site, DEGREE_CURRICULUM_PROVIDER, "Degree Curriculum");
                    break;
                case "accessRequirements":
                    migrateToWidgetPage(page, site, DEGREE_INFO_PROVIDER, "Degree Access Regimen");
                    break;
                case "professionalStatus":
                    migrateToWidgetPage(page, site, DEGREE_INFO_PROVIDER, "Degree Professional Statute");
                    break;
                case "degreeExecutionCourses":
                    migrateToWidgetPage(page, site, DEGREE_EC_PROVIDER, "Degree Execution Courses");
                    break;
                case "calendarEvents":
                    migrateToWidgetPage(page, site, DEGREE_EVALUATIONS_PROVIDER, "Degree Evaluations");
                    break;
                case "degreeClasses":
                    migrateToWidgetPage(page, site, DEGREE_CLASSES_SCHEDULE_PROVIDER, "Degree Classes Schedule");
                    break;
                case "curricularPlans":
                    taskLog("    Skipping curricularPlans page (no direct widget equivalent).");
                    break;
                case "view":
                    migrateViewPostPage(page, site);
                    break;
                default:
                    taskLog("    Unknown template type '%s', skipping page.", templateType);
                    break;
            }
        }
    }

    private void migrateExecutionCourseSite(Site site, CmsSiteType siteType) {
        taskLog("Migrating EC site: %s", site.getSlug());

        site.setSiteType(siteType);
        site.setTheme(siteType.getCmsSiteTypeTheme());

        ensureCategoryExists(site, ANNOUNCEMENT_CATEGORY_SLUG,
                new LocalizedString.Builder()
                        .with(Locale.getDefault(), "Anúncios")
                        .with(Locale.ENGLISH, "Announcements").build());
        ensureCategoryExists(site, SUMMARY_CATEGORY_SLUG,
                new LocalizedString.Builder()
                        .with(Locale.getDefault(), "Sumários")
                        .with(Locale.ENGLISH, "Summaries").build());

        for (Page page : site.getPagesSet()) {
            if (!needsMigration(page)) {
                continue;
            }

            final String templateType = page.getTemplateType();
            taskLog("  Migrating page '%s' (template: %s)", page.getName().getContent(), templateType);

            switch (templateType) {
                case "firstPage":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Initial Page");
                    break;
                case "category":
                    migrateCategoryPageExecutionCourse(page, site);
                    break;
                case "evaluations":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Evaluations");
                    break;
                case "bibliographicReferences":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Bibliographic References");
                    break;
                case "calendarEvents":
                    migrateToWidgetPage(page, site, EC_SCHEDULE_PROVIDER, "Execution Course Schedule");
                    break;
                case "evaluationMethods":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Evaluation Methods");
                    break;
                case "objectives":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Objectives");
                    break;
                case "lessonPlan":
                    migrateToWidgetPage(page, site, EC_PLANNING_PROVIDER, "Execution Course Planning");
                    break;
                case "program":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Program");
                    break;
                case "shifts":
                    migrateToWidgetPage(page, site, EC_INFO_PROVIDER, "Execution Course Shifts");
                    break;
                case "groupings":
                    taskLog("    Skipping groupings page (no direct widget equivalent).");
                    break;
                case "view":
                    migrateViewPostPage(page, site);
                    break;
                default:
                    taskLog("    Unknown template type '%s', skipping page.", templateType);
                    break;
            }
        }
    }

    private void migrateCategoryPageDegree(Page page, Site site) {
        final String categorySlug = findCategorySlugOnPage(page);
        if (ANNOUNCEMENT_CATEGORY_SLUG.equals(categorySlug)) {
            migrateToWidgetPage(page, site, POSTS_DATA_PROVIDER, "Degree Posts");
        } else {
            taskLog("    Unrecognized category page on degree site, skipping.");
        }
    }

    private void migrateCategoryPageExecutionCourse(Page page, Site site) {
        final String categorySlug = findCategorySlugOnPage(page);

        if (ANNOUNCEMENT_CATEGORY_SLUG.equals(categorySlug)) {
            migrateToWidgetPageWithProperties(page, site, EC_COMM_MESSAGES_PROVIDER,
                    "Execution Course Communication Messages", buildAnnouncementsProperties());
        } else if (SUMMARY_CATEGORY_SLUG.equals(categorySlug)) {
            migrateToWidgetPageWithProperties(page, site, POSTS_DATA_PROVIDER,
                    "Execution Course Summaries", buildSummariesProperties());
        } else {
            taskLog("    Unrecognized category page on EC site (slug: %s), skipping.", categorySlug);
        }
    }

    private Map<String, String> buildAnnouncementsProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("numOfPostsPerPage", "5");
        properties.put("paginate", "true");
        return properties;
    }

    private Map<String, String> buildSummariesProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("category", SUMMARY_CATEGORY_SLUG);
        properties.put("numOfPostsPerPage", "5");
        properties.put("title", "Sumários");
        properties.put("paginate", "true");
        return properties;
    }

    private String findCategorySlugOnPage(Page page) {
        for (Object component : page.getComponentsSet()) {
            if (component instanceof ListCategoryPosts) {
                final Category category = ((ListCategoryPosts) component).getCategory();
                if (category != null && category.getSlug() != null) {
                    return category.getSlug();
                }
            }
        }
        return null;
    }

    private void migrateToWidgetPage(Page page, Site site, String widgetDataProviderClass, String widgetName) {
        migrateToWidgetPageWithProperties(page, site, widgetDataProviderClass, widgetName, new HashMap<>());
    }

    private void migrateToWidgetPageWithProperties(Page page, Site site, String widgetDataProviderClass,
            String widgetName, Map<String, String> properties) {

        ensureStaticPost(page, site);

        removeOldComponents(page);

        page.setTemplate(singleAreaPageTemplate);

        if (!hasComponentOfType(page, CmsWidgetContainersRenderComponent.class)) {
            page.addComponents(Component.forType(CmsWidgetContainersRenderComponent.class));
        }

        if (!hasComponentOfType(page, CmsEmbeddedPagesRenderComponent.class)) {
            page.addComponents(Component.forType(CmsEmbeddedPagesRenderComponent.class));
        }

        final Optional<CmsWidgetType> existingWidgetType =
                CmsWidgetType.findAll().stream().filter(wt -> wt.getName().containsAny(widgetName)).findFirst();

        final CmsWidgetType widgetType = existingWidgetType.orElse(null);
        if (widgetType == null) {
            taskLog("    WARNING: Widget type '%s' not found. Widget container created without type.", widgetName);
        }

        if (page.getCmsWidgetContainersSet() != null && !page.getCmsWidgetContainersSet().isEmpty()) {
            taskLog("    Page already has widget containers, skipping container creation.");
            return;
        }

        singleAreaPageTemplate.getCmsWidgetAreasSet().forEach(area -> {
            final CmsWidgetContainer container = CmsWidgetContainer.create(area);

            if (widgetType != null) {
                final CmsWidget widget = CmsWidget.create(widgetType, container);
                properties.forEach((k, v) -> CmsWidgetProperty.create(widget, k, v));
                container.addWidgets(widget);
            }

            container.setPage(page);
        });
    }

    private void migrateViewPostPage(Page page, Site site) {
        page.setTemplate(viewPostEmbeddedTemplate);

        removeOldComponents(page);

        ensureStaticPost(page, site);

        if (!hasComponentOfType(page, CmsEmbeddedPagesRenderComponent.class)) {
            page.addComponents(Component.forType(CmsEmbeddedPagesRenderComponent.class));
        }
    }

    private void ensureStaticPost(Page page, Site site) {
        if (page.getStaticPost().isPresent()) {
            return;
        }

        final Post post = Post.create(site, page, page.getName(), new LocalizedString(), new LocalizedString(),
                SiteServices.findForPostContent(site), true, Authenticate.getUser());
        page.addComponents(new StaticPost(post));
    }

    private void removeOldComponents(Page page) {
        page.getComponentsSet().stream()
                .filter(c -> !isWidgetOrEmbeddedComponent(c) && !(c instanceof StaticPost))
                .collect(java.util.stream.Collectors.toList())
                .forEach(c -> {
                    page.removeComponents(c);
                    c.delete();
                });
    }

    private boolean isWidgetOrEmbeddedComponent(Object component) {
        return component instanceof CmsWidgetContainersRenderComponent
                || component instanceof CmsEmbeddedPagesRenderComponent;
    }

    private boolean hasComponentOfType(Page page, Class<?> componentClass) {
        return page.getComponentsSet().stream().anyMatch(componentClass::isInstance);
    }

    private void ensureCategoryExists(Site site, String slug, LocalizedString name) {
        site.getOrCreateCategoryForSlug(slug, name);
    }
}
