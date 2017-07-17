/*
 * Copyright (C) 2017 EfficiOS Inc., Alexandre Montplaisir <alexmonthy@efficios.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.lttng.scope.tmf2.views.ui.timeline.widgets.timegraph.toolbar.nav;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.annotation.Nullable;
import org.lttng.scope.tmf2.views.ui.timeline.widgets.timegraph.StateRectangle;
import org.lttng.scope.tmf2.views.ui.timeline.widgets.timegraph.TimeGraphWidget;

import com.efficios.jabberwocky.project.ITraceProject;
import com.efficios.jabberwocky.trace.event.ITraceEvent;

/**
 * Navigation mode using the current entry's events. It looks through all events
 * in the trace belonging to the tree entry of the current selected state, and
 * navigates through them. This allows stopping at events that may not cause a
 * state change shown in the view.
 *
 * @author Alexandre Montplaisir
 */
public class NavigationModeFollowEvents extends NavigationMode {

    private static final String BACK_ICON_PATH = "/icons/toolbar/nav_event_back.gif"; //$NON-NLS-1$
    private static final String FWD_ICON_PATH = "/icons/toolbar/nav_event_fwd.gif"; //$NON-NLS-1$

    /**
     * Mutex rule for search action jobs, making sure they execute sequentially
     */
    private final ISchedulingRule fSearchActionMutexRule = new ISchedulingRule() {
        @Override
        public boolean isConflicting(@Nullable ISchedulingRule rule) {
            return (rule == this);
        }

        @Override
        public boolean contains(@Nullable ISchedulingRule rule) {
            return (rule == this);
        }
    };

    /**
     * Constructor
     */
    public NavigationModeFollowEvents() {
        super(requireNonNull(Messages.sfFollowEventsNavModeName),
                BACK_ICON_PATH,
                FWD_ICON_PATH);
    }

    @Override
    public void navigateBackwards(TimeGraphWidget viewer) {
        navigate(viewer, false);
    }

    @Override
    public void navigateForwards(TimeGraphWidget viewer) {
        navigate(viewer, true);
    }

    @Override
    public boolean isEnabled() {
        // TODO Re-enable
        return false;
    }

    private static void navigate(TimeGraphWidget viewer, boolean forward) {
        StateRectangle state = viewer.getSelectedState();
        ITraceProject<?, ?> project = viewer.getControl().getViewContext().getCurrentTraceProject();
        if (state == null || project == null) {
            return;
        }
        Predicate<ITraceEvent> predicate = state.getStateInterval().getTreeElement().getEventMatching();
        if (predicate == null) {
            /* The tree element does not support navigating by events. */
            return;
        }

        // TODO Reimplement outside of TMF

//        String jobName = (forward ? Messages.sfNextEventJobName : Messages.sfPreviousEventJobName);
//
//        Job job = new Job(jobName) {
//            @Override
//            protected IStatus run(@Nullable IProgressMonitor monitor) {
//                long currentTime = TmfTraceManager.getInstance().getCurrentTraceContext().getSelectionRange().getStartTime().toNanos();
//                ITmfContext ctx = trace.seekEvent(TmfTimestamp.fromNanos(currentTime));
//                long rank = ctx.getRank();
//                ctx.dispose();
//
//                ITmfEvent event = (forward ?
//                        TmfTraceUtils.getNextEventMatching(trace, rank, predicate, monitor) :
//                        TmfTraceUtils.getPreviousEventMatching(trace, rank, predicate, monitor));
//                if (event != null) {
//                    NavUtils.selectNewTimestamp(viewer, event.getTimestamp().toNanos());
//                }
//                return Status.OK_STATUS;
//            }
//        };
//
//        /*
//         * Make subsequent jobs not run concurrently, but wait after one
//         * another.
//         */
//        job.setRule(fSearchActionMutexRule);
//        job.schedule();
    }
}
