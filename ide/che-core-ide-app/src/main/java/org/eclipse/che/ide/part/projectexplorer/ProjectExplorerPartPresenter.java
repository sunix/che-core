/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.part.projectexplorer;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.event.NodeExpandedEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateEvent;
import org.eclipse.che.ide.menu.ContextMenu;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
import org.eclipse.che.ide.api.event.NodeChangedEvent;
import org.eclipse.che.ide.api.event.NodeChangedHandler;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.TreeStructureProviderRegistry;
import org.eclipse.che.ide.api.project.tree.generic.Openable;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.logger.AnalyticsEventLoggerExt;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.tree.SelectionModel;
import org.eclipse.che.ide.util.loging.Log;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.util.Config;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;
import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.DELETED;

/**
 * Project Explorer displays project's tree in a dedicated part (view).
 *
 * @author Nikolay Zamosenchuk
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectExplorerPartPresenter extends BasePresenter implements ProjectExplorerView.ActionDelegate,
        ProjectExplorerPart, HasView {
    private ProjectExplorerView            view;
    private EventBus                       eventBus;
    private ContextMenu                    contextMenu;
    private ProjectServiceClient           projectServiceClient;
    private CoreLocalizationConstant       coreLocalizationConstant;
    private AppContext                     appContext;
    private TreeStructureProviderRegistry  treeStructureProviderRegistry;
    private TreeStructure                  currentTreeStructure;
    private DeleteNodeHandler              deleteNodeHandler;
    private Provider<ProjectListStructure> projectListStructureProvider;
    private AnalyticsEventLoggerExt        eventLogger;

    /** A list of nodes is used for asynchronously refreshing the tree. */
    private Array<TreeNode<?>>             nodesToRefresh;

    /** Instantiates the Project Explorer presenter. */
    @Inject
    public ProjectExplorerPartPresenter(ProjectExplorerView view,
                                        EventBus eventBus,
                                        ProjectServiceClient projectServiceClient,
                                        ContextMenu contextMenu,
                                        CoreLocalizationConstant coreLocalizationConstant,
                                        AppContext appContext,
                                        TreeStructureProviderRegistry treeStructureProviderRegistry,
                                        DeleteNodeHandler deleteNodeHandler,
                                        AnalyticsEventLoggerExt eventLogger,
                                        Provider<ProjectListStructure> projectListStructureProvider) {
        this.view = view;
        this.eventBus = eventBus;
        this.contextMenu = contextMenu;
        this.projectServiceClient = projectServiceClient;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.appContext = appContext;
        this.treeStructureProviderRegistry = treeStructureProviderRegistry;
        this.deleteNodeHandler = deleteNodeHandler;
        this.eventLogger = eventLogger;
        this.projectListStructureProvider = projectListStructureProvider;
        this.view.setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

        bind();
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen() {
        if (Config.getProjectName() == null) {
            setTree(projectListStructureProvider.get());
        } else {
            projectServiceClient.getProject(Config.getProjectName(), new AsyncRequestCallback<ProjectDescriptor>() {
                @Override
                protected void onSuccess(ProjectDescriptor result) {
                }

                @Override
                protected void onFailure(Throwable exception) {
                    setTree(projectListStructureProvider.get());
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getTitle() {
        return coreLocalizationConstant.projectExplorerButtonTitle();
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "This View helps you to do basic operation with your projects.";
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return 230;
    }

    /** Adds behavior to view's components. */
    protected void bind() {
        view.setDelegate(this);

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                final ProjectDescriptor project = event.getProject();
                setTree(treeStructureProviderRegistry.getTreeStructureProvider(project.getType()).get());
                view.setProjectHeader(event.getProject());
                eventLogger.logEvent("project-opened", new HashMap<String, String>());
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                // this isn't case when some project going to open while previously opened project is closing
                if (!event.isCloseBeforeOpening()) {
                    setTree(projectListStructureProvider.get());
                    view.hideProjectHeader();
                }
            }
        });

        eventBus.addHandler(RefreshProjectTreeEvent.TYPE, new RefreshProjectTreeHandler() {
            @Override
            public void onRefresh(RefreshProjectTreeEvent event) {
                eventBus.fireEvent(new PersistProjectTreeStateEvent());
                if (appContext.getCurrentProject() == null) {
                    setTree(projectListStructureProvider.get());
                    return;
                }

                if (event.refreshSubtree()) {
                    nodesToRefresh = view.getOpenedTreeNodes();
                    refreshTreeNodes();
                    return;
                }

                if (event.getNode() != null) {
                    refreshAndSelectNode(event.getNode());
                    return;
                }

                currentTreeStructure.getRootNodes(new AsyncCallback<Array<TreeNode<?>>>() {
                    @Override
                    public void onSuccess(Array<TreeNode<?>> result) {
                        for (TreeNode<?> childNode : result.asIterable()) {
                            // clear children in order to force to refresh
                            childNode.setChildren(Collections.<TreeNode<?>>createArray());
                            refreshAndSelectNode(childNode);

                            ProjectDescriptor projectDescriptor = appContext.getCurrentProject().getRootProject();
                            String workspaceName = projectDescriptor.getWorkspaceName();
                            String fullProjectPath = "/" + workspaceName + projectDescriptor.getPath();
                            eventBus.fireEvent(new RestoreProjectTreeStateEvent(fullProjectPath));
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error(ProjectExplorerPartPresenter.class, caught);
                    }
                });
            }
        });

        eventBus.addHandler(NodeChangedEvent.TYPE, new NodeChangedHandler() {
            @Override
            public void onNodeRenamed(NodeChangedEvent event) {
                if (appContext.getCurrentProject() == null) {
                    // any opened project - all projects list is shown
                    setTree(currentTreeStructure);
                } else {
                    updateNode(event.getNode().getParent());
                    view.selectNode(event.getNode());
                }
            }
        });

        eventBus.addHandler(ItemEvent.TYPE, new ItemHandler() {
            @Override
            public void onItem(final ItemEvent event) {
                if (DELETED == event.getOperation()) {
                    refreshAndSelectNode(event.getItem().getParent());
                } else if (CREATED == event.getOperation()) {
                    final TreeNode<?> selectedNode = view.getSelectedNode();
                    updateNode(selectedNode.getParent());
                    updateNode(selectedNode);
                    view.expandAndSelectNode(event.getItem());
                }
            }
        });
    }


    /**
     * Asynchronously refreshes all nodes from the nodesToRefresh list.
     */
    private void refreshTreeNodes() {
        if (nodesToRefresh != null && !nodesToRefresh.isEmpty()) {
            TreeNode<?> treeNode = nodesToRefresh.get(0);
            nodesToRefresh.remove(0);

            refreshNode(treeNode, new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onSuccess(TreeNode<?> result) {
                    refreshTreeNodes();
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.error(ProjectExplorerPartPresenter.class, caught);

                    // Skip current node and continue refreshing.
                    refreshTreeNodes();
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeSelected(final TreeNode< ? > node, final SelectionModel< ? > model) {
        final Array< ? > allSelected = model.getSelectedNodes();
        final List<Object> newSelection = new ArrayList<>();
        for (final Object item : allSelected.asIterable()) {
            newSelection.add(item);
        }
        if (newSelection.contains(node)) {
            setSelection(new Selection<>(newSelection, node));
        } else {
            setSelection(new Selection<>(newSelection));
        }

        if (node != null && node instanceof StorableNode && appContext.getCurrentProject() != null) {
            appContext.getCurrentProject().setProjectDescription(node.getProject().getData());
        }
    }

    @Override
    public void expandNode(TreeNode<?> node) {
        view.expandAndSelectNode(node);
        view.updateNode(node, node);

        eventBus.fireEvent(new NodeExpandedEvent());
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeExpanded(@Nonnull final TreeNode<?> node) {
        if (node.getChildren().isEmpty()) {
            // If children is empty then node may be not refreshed yet?
            node.refreshChildren(new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onSuccess(TreeNode<?> result) {
                    if (node instanceof Openable) {
                        ((Openable)node).open();
                    }
                    if (!result.getChildren().isEmpty()) {
                        updateNode(result);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.error(ProjectExplorerPartPresenter.class, caught);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeAction(@Nonnull TreeNode<?> node) {
        node.processNodeAction();
    }

    /** {@inheritDoc} */
    @Override
    public void onContextMenu(int mouseX, int mouseY) {
        contextMenu.show(mouseX, mouseY);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteKey() {
        List<StorableNode> selectedNodes = new ArrayList<>();
        for (Object node : view.getSelectedNodes().asIterable()) {
            if (node instanceof StorableNode) {
                selectedNodes.add((StorableNode) node);
            }
        }
        deleteNodeHandler.deleteNodes(selectedNodes);
    }

    /** {@inheritDoc} */
    @Override
    public void onEnterKey() {
        view.getSelectedNode().processNodeAction();
    }

    private void setTree(@Nonnull final TreeStructure treeStructure) {
        currentTreeStructure = treeStructure;
        if (appContext.getCurrentProject() != null) {
            appContext.getCurrentProject().setCurrentTree(currentTreeStructure);
        }
        treeStructure.getRootNodes(new AsyncCallback<Array<TreeNode<?>>>() {
            @Override
            public void onSuccess(Array<TreeNode<?>> result) {
                view.setRootNodes(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, caught.getMessage());
            }
        });
    }

    public void refreshNode(TreeNode<?> node, final AsyncCallback<TreeNode<?>> callback) {
        node.refreshChildren(new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                updateNode(result);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    private void refreshAndSelectNode(TreeNode<?> node) {
        refreshNode(node, new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                view.selectNode(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, caught);
            }
        });
    }

    // TODO remove this method in the nearest feature
    // TODO just use updateNode of view
    private void updateNode(TreeNode<?> node) {
        view.updateNode(node, node);
    }

}
