/*******************************************************************************
 * Copyright (c) 2011, 2016 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Matthew Khouzam - Added import functionalities
 *   Geneviève Bastien - Added support for experiment types
 *   Bernd Hufmann - Updated custom trace type ID handling
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.project.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.activator.internal.Activator;
import org.eclipse.tracecompass.tmf.core.project.model.internal.Messages;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Utility class for accessing TMF trace type extensions from the platform's
 * extensions registry.
 *
 * @author Patrick Tasse
 * @author Matthew Khouzam
 */
public final class TmfTraceType {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    /** Extension point ID */
    public static final String TMF_TRACE_TYPE_ID = "org.eclipse.linuxtools.tmf.core.tracetype"; //$NON-NLS-1$

    /** Extension point element 'Category' */
    public static final String CATEGORY_ELEM = "category"; //$NON-NLS-1$

    /** Extension point element 'Type' */
    public static final String TYPE_ELEM = "type"; //$NON-NLS-1$

    /** Extension point element 'Experiment' */
    public static final String EXPERIMENT_ELEM = "experiment"; //$NON-NLS-1$

    /** Extension point attribute 'ID' */
    public static final String ID_ATTR = "id"; //$NON-NLS-1$

    /** Extension point attribute 'name' */
    public static final String NAME_ATTR = "name"; //$NON-NLS-1$

    /** Extension point attribute 'category' */
    public static final String CATEGORY_ATTR = "category"; //$NON-NLS-1$

    /** Extension point attribute 'trace_type' */
    public static final String TRACE_TYPE_ATTR = "trace_type"; //$NON-NLS-1$

    /** Extension point attribute 'event_type' */
    public static final String EVENT_TYPE_ATTR = "event_type"; //$NON-NLS-1$

    /** Extension point attribute 'experiment_type' */
    public static final String EXPERIMENT_TYPE_ATTR = "experiment_type"; //$NON-NLS-1$

    /** Extension point attribute 'isDirectory' */
    public static final String IS_DIR_ATTR = "isDirectory"; //$NON-NLS-1$

    /** Default experiment type */
    public static final String DEFAULT_EXPERIMENT_TYPE = "org.eclipse.linuxtools.tmf.core.experiment.generic"; //$NON-NLS-1$

    // The mapping of available trace type IDs to their corresponding
    // configuration element
    private static final Map<String, IConfigurationElement> TRACE_TYPE_ATTRIBUTES = new HashMap<>();
    private static final Map<String, IConfigurationElement> TRACE_CATEGORIES = new HashMap<>();
    private static final Map<String, TraceTypeHelper> TRACE_TYPES = new LinkedHashMap<>();

    static {
        populateCategoriesAndTraceTypes();
    }

    /**
     * Enum to say whether a type applies to a trace or experiment
     *
     * @author Geneviève Bastien
     */
    public enum TraceElementType {
        /** Trace type applies to trace */
        TRACE,
        /** Trace type applies to experiment */
        EXPERIMENT,
    }

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    private TmfTraceType() {
    }

    // ------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------

    /**
     * Get an iterable view of the existing trace type helpers.
     *
     * @return The currently registered trace type helpers
     */
    public static Iterable<TraceTypeHelper> getTraceTypeHelpers() {
        return TRACE_TYPES.values();
    }

    /**
     * Returns a list of trace type labels "category : name", ...
     *
     * Returns only trace types, not experiment types
     *
     * @return a list of trace type labels
     */
    public static String[] getAvailableTraceTypes() {
        return getAvailableTraceTypes(null);
    }

    /**
     * Returns a list of trace type labels "category : name", ... sorted by
     * given comparator.
     *
     * Returns only trace types, not experiment types
     *
     * @param comparator
     *            Comparator class (type String) or null for alphabetical order.
     * @return a list of trace type labels sorted according to the given
     *         comparator
     */
    public static String[] getAvailableTraceTypes(Comparator<String> comparator) {

        // Generate the list of Category:TraceType to populate the ComboBox
        List<String> traceTypes = new ArrayList<>();

        for (TraceTypeHelper tt : TRACE_TYPES.values()) {
            if (!tt.isExperimentType()) {
                traceTypes.add(tt.getLabel());
            }
        }

        if (comparator == null) {
            Collections.sort(traceTypes);
        } else {
            Collections.sort(traceTypes, comparator);
        }

        // Format result
        return traceTypes.toArray(new String[traceTypes.size()]);
    }

    /**
     * Gets a trace type for a given canonical id
     *
     * @param id
     *            the ID of the trace
     * @return the return type
     */
    public static TraceTypeHelper getTraceType(String id) {
        return TRACE_TYPES.get(id);
    }

    private static void populateCategoriesAndTraceTypes() {
        if (TRACE_TYPES.isEmpty()) {
            // Populate the Categories and Trace Types
            IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(TmfTraceType.TMF_TRACE_TYPE_ID);
            for (IConfigurationElement ce : config) {
                String elementName = ce.getName();
                if (elementName.equals(TmfTraceType.TYPE_ELEM)) {
                    String traceTypeId = ce.getAttribute(TmfTraceType.ID_ATTR);
                    TRACE_TYPE_ATTRIBUTES.put(traceTypeId, ce);
                } else if (elementName.equals(TmfTraceType.CATEGORY_ELEM)) {
                    String categoryId = ce.getAttribute(TmfTraceType.ID_ATTR);
                    TRACE_CATEGORIES.put(categoryId, ce);
                } else if (elementName.equals(TmfTraceType.EXPERIMENT_ELEM)) {
                    String experimentTypeId = ce.getAttribute(TmfTraceType.ID_ATTR);
                    TRACE_TYPE_ATTRIBUTES.put(experimentTypeId, ce);
                }
            }
            // create the trace types
            for (Entry<String, IConfigurationElement> entry : TRACE_TYPE_ATTRIBUTES.entrySet()) {
                IConfigurationElement ce = entry.getValue();
                final String category = getCategory(ce);
                final String attribute = ce.getAttribute(TmfTraceType.NAME_ATTR);
                ITmfTrace trace = null;
                TraceElementType elementType = TraceElementType.TRACE;
                try {
                    if (ce.getName().equals(TmfTraceType.TYPE_ELEM)) {
                        trace = (ITmfTrace) ce.createExecutableExtension(TmfTraceType.TRACE_TYPE_ATTR);
                    } else if (ce.getName().equals(TmfTraceType.EXPERIMENT_ELEM)) {
                        trace = (ITmfTrace) ce.createExecutableExtension(TmfTraceType.EXPERIMENT_TYPE_ATTR);
                        elementType = TraceElementType.EXPERIMENT;
                    }
                    if (trace == null) {
                        break;
                    }
                    // Deregister trace as signal handler because it is only
                    // used for validation
                    TmfSignalManager.deregister(trace);

                    final String dirString = ce.getAttribute(TmfTraceType.IS_DIR_ATTR);
                    boolean isDir = Boolean.parseBoolean(dirString);

                    final String typeId = entry.getKey();
                    TraceTypeHelper tt = new TraceTypeHelper(typeId, category, attribute, trace, isDir, elementType);
                    TRACE_TYPES.put(typeId, tt);
                } catch (CoreException e) {
                    Activator.logError("Unexpected error during populating trace types", e); //$NON-NLS-1$
                }

            }
        }
    }

    private static String getCategory(IConfigurationElement ce) {
        final String categoryId = ce.getAttribute(TmfTraceType.CATEGORY_ATTR);
        if (categoryId != null) {
            IConfigurationElement category = TRACE_CATEGORIES.get(categoryId);
            if (category != null && !category.getName().equals("")) { //$NON-NLS-1$
                return category.getAttribute(TmfTraceType.NAME_ATTR);
            }
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Get a configuration element for a given name
     *
     * @param traceType
     *            the name canonical
     * @return the configuration element, can be null
     */
    public static IConfigurationElement getTraceAttributes(String traceType) {
        return TRACE_TYPE_ATTRIBUTES.get(traceType);
    }

    /**
     * Find the id of a trace type by its label "category : name"
     *
     * @param label
     *            the trace type label
     * @return the trace type id
     */
    public static String getTraceTypeId(String label) {
        for (Entry<String, TraceTypeHelper> entry : TRACE_TYPES.entrySet()) {
            if (entry.getValue().getLabel().equals(label)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Checks if a trace is a valid directory trace
     *
     * @param path
     *            the file name (and path)
     * @return <code>true</code> if the trace is a valid directory trace else
     *         <code>false</code>
     */
    public static boolean isDirectoryTrace(String path) {
        final Iterable<TraceTypeHelper> traceTypeHelpers = getTraceTypeHelpers();
        for (TraceTypeHelper traceTypeHelper : traceTypeHelpers) {
            if (traceTypeHelper.isDirectoryTraceType() &&
                    (traceTypeHelper.validate(path).getSeverity() != IStatus.ERROR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param traceType
     *            the trace type
     * @return <code>true</code> it is a directory trace type else else
     *         <code>false</code>
     */
    public static boolean isDirectoryTraceType(String traceType) {
        if (traceType != null) {
            TraceTypeHelper traceTypeHelper = getTraceType(traceType);
            if (traceTypeHelper != null) {
                return traceTypeHelper.isDirectoryTraceType();
            }
            return false;
        }
        throw new IllegalArgumentException("Invalid trace type string: " + traceType); //$NON-NLS-1$
    }

    /**
     * Get the trace type id for a resource
     *
     * @param resource
     *            the resource
     * @return the trace type id or null if it is not set
     * @throws CoreException
     *             if the trace type id cannot be accessed
     */
    public static String getTraceTypeId(IResource resource) throws CoreException {
        String traceTypeId = resource.getPersistentProperties().get(TmfCommonConstants.TRACETYPE);
        return buildCompatibilityTraceTypeId(traceTypeId);
    }

    /**
     * This methods builds a trace type ID from a given ID taking into
     * consideration any format changes that were done for the IDs of custom
     * text or XML traces. For example, such format change took place when
     * moving to Trace Compass. Trace type IDs that are part of the plug-in
     * extension for trace types won't be changed.
     *
     * This method is useful for IDs that were persisted in the workspace before
     * the format changes (e.g. in the persistent properties of a trace
     * resource).
     *
     * It ensures backwards compatibility of the workspace for custom text and
     * XML traces.
     *
     * @param traceTypeId
     *            the legacy trace type ID
     * @return the trace type ID in Trace Compass format
     */
    public static String buildCompatibilityTraceTypeId(String traceTypeId) {
        return traceTypeId;
    }

    /**
     * This method figures out the trace type of a given trace.
     *
     * @param path
     *            The path of trace to import (file or directory for directory traces)
     * @param traceTypeHint
     *            the ID of a trace (like "o.e.l.specifictrace" )
     * @return a list of {@link TraceTypeHelper} sorted by confidence (highest first)
     *
     * @throws TmfTraceImportException
     *             if there are errors in the trace file or no trace type found
     *             for a directory trace
     * @since 2.0
     */
    public static @NonNull List<TraceTypeHelper> selectTraceType(String path, String traceTypeHint) throws TmfTraceImportException {

        Comparator<Pair<Integer, TraceTypeHelper>> comparator = new Comparator<Pair<Integer, TraceTypeHelper>>() {
            @Override
            public int compare(Pair<Integer, TraceTypeHelper> o1, Pair<Integer, TraceTypeHelper> o2) {
                int res = -o1.getFirst().compareTo(o2.getFirst()); // invert so that highest confidence is first
                if (res == 0) {
                    res = o1.getSecond().getName().compareTo(o2.getSecond().getName());
                }
                return res;
            }
        };

        TreeSet<Pair<Integer, TraceTypeHelper>> validCandidates = new TreeSet<>(comparator);
        final Iterable<TraceTypeHelper> traceTypeHelpers = TmfTraceType.getTraceTypeHelpers();
        for (TraceTypeHelper traceTypeHelper : traceTypeHelpers) {
            if (traceTypeHelper.isExperimentType()) {
                continue;
            }
            int confidence = traceTypeHelper.validateWithConfidence(path);
            if (confidence >= 0) {
                // insert in the tree map, ordered by confidence (highest confidence first) then name
                Pair<Integer, TraceTypeHelper> element = new Pair<>(confidence, traceTypeHelper);
                validCandidates.add(element);
            }
        }

        List<TraceTypeHelper> returned = new ArrayList<>();
        if (validCandidates.isEmpty()) {
            File traceFile = new File(path);
            if (traceFile.isFile()) {
                return returned;
            }
            final String errorMsg = NLS.bind(Messages.TmfOpenTraceHelper_NoTraceTypeMatch, path);
            throw new TmfTraceImportException(errorMsg);
        }

        if (validCandidates.size() != 1) {
            List<Pair<Integer, TraceTypeHelper>> candidates = new ArrayList<>(validCandidates);
            List<Pair<Integer, TraceTypeHelper>> reducedCandidates = reduce(candidates);
            for (Pair<Integer, TraceTypeHelper> candidatePair : reducedCandidates) {
                TraceTypeHelper candidate = candidatePair.getSecond();
                if (candidate.getTraceTypeId().equals(traceTypeHint)) {
                    returned.add(candidate);
                    break;
                }
            }
            if (returned.size() == 0) {
                if (reducedCandidates.size() == 0) {
                    throw new TmfTraceImportException("Error reducing trace type candidates"); //$NON-NLS-1$
                } else if (reducedCandidates.size() == 1) {
                    // Don't select the trace type if it has the lowest confidence
                    if (reducedCandidates.get(0).getFirst() > 0) {
                        returned.add(reducedCandidates.get(0).getSecond());
                    }
                } else {
                    for (Pair<Integer, TraceTypeHelper> candidatePair : reducedCandidates) {
                        // Don't select the trace type if it has the lowest confidence
                        if (candidatePair.getFirst() > 0) {
                            returned.add(candidatePair.getSecond());
                        }
                    }
                }
            }
        } else {
            // Don't select the trace type if it has the lowest confidence
            if (validCandidates.first().getFirst() > 0) {
                returned.add(validCandidates.first().getSecond());
            }
        }
        return returned;
    }

    private static List<Pair<Integer, TraceTypeHelper>> reduce(List<Pair<Integer, TraceTypeHelper>> candidates) {
        List<Pair<Integer, TraceTypeHelper>> retVal = new ArrayList<>();

        // get all the tracetypes that are unique in that stage
        for (Pair<Integer, TraceTypeHelper> candidatePair : candidates) {
            TraceTypeHelper candidate = candidatePair.getSecond();
            if (isUnique(candidate, candidates)) {
                retVal.add(candidatePair);
            }
        }
        return retVal;
    }

    /*
     * Only return the leaves of the trace types. Ignore custom trace types.
     */
    private static boolean isUnique(TraceTypeHelper trace, List<Pair<Integer, TraceTypeHelper>> set) {
        // check if the trace type is the leaf. we make an instance of the trace
        // type and if it is only an instance of itself, it is a leaf
        final ITmfTrace tmfTrace = trace.getTrace();
        int count = -1;
        for (Pair<Integer, TraceTypeHelper> child : set) {
            final ITmfTrace traceCandidate = child.getSecond().getTrace();
            if (tmfTrace.getClass().isInstance(traceCandidate)) {
                count++;
            }
        }
        return count == 0;
    }

}
