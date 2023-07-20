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
package org.fenixedu.learning.servlets;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses;
import org.fenixedu.academic.service.services.teacher.PublishMarks;
import org.fenixedu.academic.service.services.teacher.PublishMarks.MarkPublishingBean;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.MenuItem;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.PostFile;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.StaticPost;
import org.fenixedu.cms.routing.CMSRenderer;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.learning.FenixEduLearningConfiguration;
import org.fenixedu.learning.domain.degree.DegreeRequestHandler;
import org.fenixedu.learning.domain.degree.DegreeSiteListener;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseListener;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseRequestHandler;
import org.fenixedu.learning.domain.executionCourse.SummaryListener;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduLearningContextListener implements ServletContextListener {

    private final static Logger logger = LoggerFactory.getLogger(FenixEduLearningContextListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent sce) {

        if (!Boolean.TRUE.equals(FenixEduLearningConfiguration.getConfiguration().getDomainListenersEnabled())) {
            return;
        }

        Signal.register(Summary.CREATE_SIGNAL, (final DomainObjectEvent<Summary> event) -> {
            Summary summary = event.getInstance();
            SummaryListener.updatePost(new Post(summary.getExecutionCourse().getSite()), summary);
        });
        FenixFramework.getDomainModel().registerDeletionListener(Summary.class, (summary) -> {
            Post post = summary.getPost();
            if (post != null) {
                summary.setPost(null);
                post.delete();
            }
        });
        Signal.register(Summary.EDIT_SIGNAL, (final DomainObjectEvent<Summary> event) -> {
            SummaryListener.updatePost(event.getInstance().getPost(), event.getInstance());
        });
        Signal.register(ExecutionCourse.CREATED_SIGNAL, (final DomainObjectEvent<ExecutionCourse> event) -> {
            ExecutionCourseListener.create(event.getInstance());
        });

        Signal.register(ExecutionCourse.ACRONYM_CHANGED_SIGNAL, (final DomainObjectEvent<ExecutionCourse> event) -> {
            ExecutionCourseListener.updateSiteSlug(event.getInstance());
        });

        Signal.register(Degree.CREATED_SIGNAL, (final DomainObjectEvent<Degree> event) -> {
            DegreeSiteListener.create(event.getInstance());
        });

        Signal.register(PublishMarks.MARKS_PUBLISHED_SIGNAL, FenixEduLearningContextListener::handleMarksPublishment);
        Signal.register(Site.SIGNAL_EDITED, FenixEduLearningContextListener::handleSiteEdition);
        FenixFramework.getDomainModel().registerDeletionListener(ExecutionCourse.class, (executionCourse) -> {
            if (executionCourse.getSite() != null) {
                Site site = executionCourse.getSite();
                executionCourse.setSite(null);
                site.delete();
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Degree.class, (degree) -> {
            if (degree.getSite() != null) {
                Site site = degree.getSite();
                degree.setSite(null);
                site.delete();
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Site.class, (site) -> {
            if (site.getSystemMenu() != null) {
                Menu menu = site.getSystemMenu();
                site.setSystemMenu(null);
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Menu.class, (menu) -> {
            if (menu.getSystemSite() != null) {
                menu.setSystemSite(null);
            }
        });

        MergeExecutionCourses.registerMergeHandler(FenixEduLearningContextListener::copyExecutionCoursesSites);

        CMSRenderer.addHandler(new ExecutionCourseRequestHandler());
        CMSRenderer.addHandler(new DegreeRequestHandler());

    }

    private static void handleSiteEdition(final DomainObjectEvent<Site> site) {
        if (site.getInstance().getExecutionCourse() != null) {
            site.getInstance().getExecutionCourse().setSiteUrl(site.getInstance().getFullUrl());
        }
    }

    private static void copyExecutionCoursesSites(final ExecutionCourse from, final ExecutionCourse to) {
        if (from.getSite() != null) {
            if (to.getSite() != null) {
                Menu newMenu = to.getSite().getMenusSet().stream().findAny().get();

                LocalizedString newPageName = new LocalizedString().with(Locale.getDefault(),
                        from.getName() + "(" + from.getDegreePresentationString() + ")");

                MenuItem emptyPageParent = PagesAdminService
                        .create(to.getSite(), null, newPageName, new LocalizedString(), new LocalizedString()).get();

                emptyPageParent.getPage().setPublished(false);
                emptyPageParent.setTop(newMenu);

                for (Menu oldMenu : from.getSite().getMenusSet()) {
                    oldMenu.getToplevelItemsSorted().forEach(
                            menuItem -> PagesAdminService.copyStaticPage(menuItem, to.getSite(), newMenu, emptyPageParent));
                }
            } else {
                to.setSite(from.getSite());
                from.setSite(null);
            }
        }
    }

    private static void handleMarksPublishment(final MarkPublishingBean bean) {
        String publishmentMessage = bean.getEvaluation().getPublishmentMessage();
        if (publishmentMessage != null && !Strings.isNullOrEmpty(publishmentMessage.trim())
                && bean.getCourse().getSite() != null) {
            Category cat = bean.getCourse().getSite().categoryForSlug("announcement");
            if (cat != null) {
                Post post = new Post(bean.getCourse().getSite());
                post.addCategories(cat);
                post.setName(bean.getTitle() == null ? BundleUtil.getLocalizedString("resources.ApplicationResources",
                        "message.publishment") : new LocalizedString(I18N.getLocale(), bean.getTitle()));
                post.setBody(new LocalizedString(I18N.getLocale(), bean.getEvaluation().getPublishmentMessage()));
                post.setActive(true);
            }
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {

    }

    private static class PagesAdminService {

        @Atomic(mode = Atomic.TxMode.WRITE)
        protected static Optional<MenuItem> create(final Site site, final MenuItem parent, final LocalizedString name,
                final LocalizedString body, final LocalizedString excerpt) {
            Menu menu = site.getMenusSet().stream().findFirst().orElse(null);
            Page page = Page.create(site, menu, parent, Post.sanitize(name), true, "view", Authenticate.getUser());
            Category category =
                    site.getOrCreateCategoryForSlug("content", new LocalizedString().with(I18N.getLocale(), "Content"));
            Post post = Post.create(site, page, Post.sanitize(name), Post.sanitize(body), Post.sanitize(excerpt), category, true,
                    Authenticate.getUser());
            page.addComponents(new StaticPost(post));
            MenuItem menuItem = page.getMenuItemsSet().stream().findFirst().get();
            if (parent != null) {
                parent.add(menuItem);
            } else {
                menu.add(menuItem);
            }
            return Optional.of(menuItem);
        }

        protected static void copyStaticPage(final MenuItem oldMenuItem, final Site newSite, final Menu newMenu,
                final MenuItem newParent) {
            if (oldMenuItem.getPage() != null) {
                Page oldPage = oldMenuItem.getPage();
                staticPost(oldPage).ifPresent(oldPost -> {
                    Page newPage = new Page(newSite, oldPage.getName());
                    newPage.setTemplate(newSite.getTheme().templateForType(oldPage.getTemplate().getType()));
                    newPage.setCreatedBy(Authenticate.getUser());
                    newPage.setPublished(false);

                    for (Component component : oldPage.getComponentsSet()) {
                        if (component instanceof StaticPost) {
                            StaticPost staticPostComponent = (StaticPost) component;
                            Post newPost = clonePost(staticPostComponent.getPost(), newSite);
                            newPost.setActive(true);
                            StaticPost newComponent = new StaticPost(newPost);
                            newPage.addComponents(newComponent);
                        }
                    }

                    MenuItem newMenuItem = MenuItem.create(newMenu, newPage, oldMenuItem.getName(), newParent);
                    newMenuItem.setPosition(oldMenuItem.getPosition());
                    newMenuItem.setUrl(oldMenuItem.getUrl());
                    newMenuItem.setFolder(oldMenuItem.getFolder());

                    oldMenuItem.getChildrenSet().stream().forEach(child -> copyStaticPage(child, newSite, newMenu, newMenuItem));
                });
            }
        }

        private static Post clonePost(final Post oldPost, final Site newSite) {
            Post newPost = new Post(newSite);
            newPost.setName(oldPost.getName());
            newPost.setBodyAndExcerpt(oldPost.getBody(), oldPost.getExcerpt());
            newPost.setCreationDate(new DateTime());
            newPost.setCreatedBy(Authenticate.getUser());
            newPost.setActive(oldPost.getActive());

            for (Category oldCategory : oldPost.getCategoriesSet()) {
                Category newCategory = newSite.getOrCreateCategoryForSlug(oldCategory.getSlug(), oldCategory.getName());
                newPost.addCategories(newCategory);
            }

            oldPost.getFilesSet().stream().map(postFile -> postFile.getFiles()).forEach(file -> {
                try {
                    new PostFile(newPost,
                            new GroupBasedFile(file.getDisplayName(), file.getFilename(), file.getStream(), Group.anyone()),
                            false, file.getPostFile().getIndex());
                } catch (IOException e) {
                    logger.warn("could not clone file " + file.getDisplayName() + " from post " + newPost.getSlug() + " on site "
                            + newPost.getSite().getSlug());
                }
            });

            return newPost;
        }

        private static Optional<Post> staticPost(final Page page) {
            return page.getComponentsSet().stream().filter(StaticPost.class::isInstance).map(StaticPost.class::cast)
                    .map(StaticPost::getPost).findFirst();
        }

    }
}
