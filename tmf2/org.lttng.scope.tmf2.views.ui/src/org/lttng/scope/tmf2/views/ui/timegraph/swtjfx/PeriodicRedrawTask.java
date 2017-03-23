/*
 * Copyright (C) 2017 EfficiOS Inc., Alexandre Montplaisir <alexmonthy@efficios.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.lttng.scope.tmf2.views.ui.timegraph.swtjfx;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.lttng.scope.tmf2.views.ui.timegraph.swtjfx.Position.HorizontalPosition;
import org.lttng.scope.tmf2.views.ui.timegraph.swtjfx.Position.VerticalPosition;

/**
 * It was quickly determined that having mouse listeners start repaint tasks
 * directly is not a good solution, since simply enqueuing a job can potentially
 * take time, and thus makes scrolling sluggish and annoying to use.
 *
 * Instead, a separate thread can look periodically if the current view's
 * position has changed since its last check, and queue UI updates as needed.
 *
 * This class implements such thread, as a {@link TimerTask}.
 *
 * @author Alexandre Montplaisir
 */
class PeriodicRedrawTask extends TimerTask {

    /**
     * Sequence number attached to each redraw operation. Can be used for
     * tracing/analysis.
     */
    private final AtomicLong fTaskSeq = new AtomicLong();
    private final SwtJfxTimeGraphViewer fViewer;


    private HorizontalPosition fPreviousHorizontalPos = new HorizontalPosition(0, 0);
    private VerticalPosition fPreviousVerticalPosition = new VerticalPosition(0, 0, 0);

    public PeriodicRedrawTask(SwtJfxTimeGraphViewer viewer) {
        fViewer = viewer;
    }

    @Override
    public void run() {
        if (!fViewer.getDebugOptions().isPaintingEnabled()) {
            return;
        }

        HorizontalPosition currentHorizontalPos = fViewer.getCurrentHorizontalPosition();
        VerticalPosition currentVerticalPos = fViewer.getCurrentVerticalPosition();

        if (currentHorizontalPos.equals(fPreviousHorizontalPos)
                && currentVerticalPos.equals(fPreviousVerticalPosition)) {
            /*
             * Exact same position as the last one we've seen, no need to
             * repaint.
             */
            return;
        }
        fPreviousHorizontalPos = currentHorizontalPos;
        fPreviousVerticalPosition = currentVerticalPos;

        fViewer.paintBackground(currentVerticalPos);
        fViewer.paintArea(currentHorizontalPos, currentVerticalPos, fTaskSeq.getAndIncrement());
    }

}