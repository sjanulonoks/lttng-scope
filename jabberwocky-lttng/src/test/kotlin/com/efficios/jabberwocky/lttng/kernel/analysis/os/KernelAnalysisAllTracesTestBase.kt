/*
 * Copyright (C) 2018 EfficiOS Inc., Alexandre Montplaisir <alexmonthy@efficios.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.efficios.jabberwocky.lttng.kernel.analysis.os

import ca.polymtl.dorsal.libdelorean.IStateSystemReader
import ca.polymtl.dorsal.libdelorean.exceptions.AttributeNotFoundException
import com.efficios.jabberwocky.lttng.testutils.ExtractedCtfTestTrace
import com.efficios.jabberwocky.project.TraceProject
import com.efficios.jabberwocky.tests.JavaFXTestBase
import com.google.common.io.MoreFiles

import org.junit.jupiter.api.*
import org.lttng.scope.ttt.ctf.CtfTestTrace
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal abstract class KernelAnalysisAllTracesTestBase(private val testTrace: ExtractedCtfTestTrace) : JavaFXTestBase() {

    companion object {
        private const val PROJECT_NAME = "kernel-analysis-test-project"
        private val ANALYSIS = KernelAnalysis
    }

    private var projectPath: Path? = null
    private var project: TraceProject<*, *>? = null
    private var ss: IStateSystemReader? = null

    @BeforeEach
    fun setUp() {
        try {
            projectPath = Files.createTempDirectory(PROJECT_NAME)
        } catch (e: IOException) {
            fail(e.message)
        }

        project = TraceProject.ofSingleTrace(PROJECT_NAME, projectPath!!, testTrace.trace)
        ss = ANALYSIS.execute(project!!, null, null)
    }

    @AfterEach
    fun tearDown() {
        ss?.dispose()
        projectPath?.let {
            try {
                MoreFiles.deleteRecursively(it)
            } catch (e: IOException) {
                /* Ignore */
            }
        }
    }

    /** Ensure there is no "-1" thread generated by the state provider. */
    @Test
    fun testNoMinusOneAttribute() {
        val ss = ss!!
        assertThrows<AttributeNotFoundException> { ss.getQuarkAbsolute("Threads", "-1") }
    }
}

internal class ContextSwitchesTest : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.CONTEXT_SWITCHES_KERNEL)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}

internal class KernelTest : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.KERNEL)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}

internal class KernelVMTest : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.KERNEL_VM)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}

internal class ManyThreadsTest : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.MANY_THREADS)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}

internal class Trace2Test : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.TRACE2)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}

internal class UnevenStreamsTest : KernelAnalysisAllTracesTestBase(TRACE) {
    companion object {
        private lateinit var TRACE: ExtractedCtfTestTrace

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            TRACE = ExtractedCtfTestTrace(CtfTestTrace.UNEVEN_STREAMS)
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            TRACE.close()
        }
    }
}