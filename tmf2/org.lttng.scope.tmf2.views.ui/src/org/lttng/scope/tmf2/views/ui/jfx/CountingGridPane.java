/*
 * Copyright (C) 2017 EfficiOS Inc., Alexandre Montplaisir <alexmonthy@efficios.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.lttng.scope.tmf2.views.ui.jfx;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/**
 * Extension of a {@link GridPane} which tracks the number of inserted rows.
 *
 * Make sure you only add rows to the pane using {@link #appendRow(Node...)} or
 * else it will give weird results!
 *
 * @author Alexandre Montplaisir
 */
public class CountingGridPane extends GridPane {

    private int fNbRows = 0;

    /**
     * Add a row of nodes to this grid pane. Not thread-safe!
     *
     * @param children
     *            The contents of the row
     */
    public void appendRow(Node... children) {
        addRow(fNbRows++, children);
    }
}
