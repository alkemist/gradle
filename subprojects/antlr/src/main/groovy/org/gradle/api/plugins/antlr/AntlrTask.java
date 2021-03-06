/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.antlr;

import org.gradle.api.file.FileCollection;

import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import org.gradle.api.GradleException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.gradle.api.plugins.antlr.internal.AntlrWorkerManager;
import org.gradle.api.plugins.antlr.internal.AntlrSpec;
import org.gradle.api.plugins.antlr.internal.AntlrResult;

import org.gradle.internal.Factory;
import org.gradle.process.internal.WorkerProcessBuilder;

import javax.inject.Inject;

/**
 * Generates parsers from Antlr grammars.
 */
public class AntlrTask extends SourceTask {

    private boolean trace;
    private boolean traceLexer;
    private boolean traceParser;
    private boolean traceTreeWalker;
    private List<String> arguments = new ArrayList<String>();

    private FileCollection antlrClasspath;

    private File outputDirectory;
    private File sourceDirectory;
    private String maxHeapSize = "1g";

    /**
     * Specifies that all rules call {@code traceIn}/{@code traceOut}.
     */
    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * Specifies that all lexer rules call {@code traceIn}/{@code traceOut}.
     */
    public boolean isTraceLexer() {
        return traceLexer;
    }

    public void setTraceLexer(boolean traceLexer) {
        this.traceLexer = traceLexer;
    }

    /**
     * Specifies that all parser rules call {@code traceIn}/{@code traceOut}.
     */
    public boolean isTraceParser() {
        return traceParser;
    }

    public void setTraceParser(boolean traceParser) {
        this.traceParser = traceParser;
    }

    /**
     * Specifies that all tree walker rules call {@code traceIn}/{@code traceOut}.
     */
    public boolean isTraceTreeWalker() {
        return traceTreeWalker;
    }

    public void setTraceTreeWalker(boolean traceTreeWalker) {
        this.traceTreeWalker = traceTreeWalker;
    }

    public String getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(String maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public void setArguments(List<String> arguments) {
        if (arguments != null) {
            this.arguments = arguments;
        }
    }

    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns the directory to generate the parser source files into.
     *
     * @return The output directory.
     */
    @OutputDirectory
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Specifies the directory to generate the parser source files into.
     *
     * @param outputDirectory The output directory. Must not be null.
     */
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * Returns the classpath containing the Ant ANTLR task implementation.
     *
     * @return The Ant task implementation classpath.
     */
    @InputFiles
    public FileCollection getAntlrClasspath() {
        return antlrClasspath;
    }

    /**
     * Specifies the classpath containing the Ant ANTLR task implementation.
     *
     * @param antlrClasspath The Ant task implementation classpath. Must not be null.
     */
    public void setAntlrClasspath(FileCollection antlrClasspath) {
        this.antlrClasspath = antlrClasspath;
    }

    @Inject
    public Factory<WorkerProcessBuilder> getWorkerProcessBuilderFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void generate() {
        AntlrWorkerManager manager = new AntlrWorkerManager();
        List<String> args = buildArguments();
        AntlrSpec spec = new AntlrSpec(args, maxHeapSize);
        AntlrResult result = manager.runWorker(getProject().getProjectDir(), getWorkerProcessBuilderFactory(), getAntlrClasspath(), spec);
        evaluateAntlrResult(result);
    }

    public void evaluateAntlrResult(AntlrResult result) {
        int errorCount = result.getErrorCount();
        if (errorCount == 1) {
            throw new GradleException("There was 1 error during grammar generation");
        } else if (errorCount > 1) {
            throw new GradleException("There were "
                + errorCount
                + " errors during grammar generation");
        }
    }

    /**
     * Finalizes the list of arguments that will be sent to the ANTLR tool.
     */
    List<String> buildArguments() {
        List<String> args = new ArrayList<String>();    // List for finalized arguments
        
        // Output file
        args.add("-o");
        args.add(outputDirectory.getAbsolutePath());
        
        // Custom arguments
        for (String argument : arguments) {
            args.add(argument);
        }

        // Add trace parameters, if they don't already exist
        if (isTrace() && !arguments.contains("-trace")) {
            args.add("-trace");
        }
        if (isTraceLexer() && !arguments.contains("-traceLexer")) {
            args.add("-traceLexer");
        }
        if (isTraceParser() && !arguments.contains("-traceParser")) {
            args.add("-traceParser");
        }
        if (isTraceTreeWalker() && !arguments.contains("-traceTreeWalker")) {
            args.add("-traceTreeWalker");
        }

        // Files in source directory
        for (File file : sourceDirectory.listFiles()) {
            args.add(file.getAbsolutePath());
        }

        return args;
    }
}
