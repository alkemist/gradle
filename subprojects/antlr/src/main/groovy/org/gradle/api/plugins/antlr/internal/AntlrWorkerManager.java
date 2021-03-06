/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.plugins.antlr.internal;

import org.gradle.api.file.FileCollection;
import org.gradle.internal.Factory;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AntlrWorkerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntlrWorkerServer.class);

    public AntlrResult runWorker(File workingDir, Factory<WorkerProcessBuilder> workerFactory, FileCollection antlrClasspath, AntlrSpec spec) {

        WorkerProcess process = createWorkerProcess(workingDir, workerFactory, antlrClasspath, spec);
        process.start();

        AntlrWorkerClient clientCallBack = new AntlrWorkerClient();
        process.getConnection().addIncoming(AntlrWorkerClientProtocol.class, clientCallBack);
        process.getConnection().connect();

        process.waitForStop();

        return clientCallBack.getResult();
    }

    private WorkerProcess createWorkerProcess(File workingDir, Factory<WorkerProcessBuilder> workerFactory, FileCollection antlrClasspath, AntlrSpec spec) {

        WorkerProcessBuilder builder = workerFactory.create();
        builder.setBaseName("Gradle ANTLR Worker");

        if (antlrClasspath != null) {
            builder.applicationClasspath(antlrClasspath);
        }
        builder.sharedPackages(new String[] {"antlr", "org.antlr"});
        JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
        javaCommand.setWorkingDir(workingDir);
        javaCommand.setMaxHeapSize(spec.getMaxHeapSize());

        return builder.worker(new AntlrWorkerServer(spec)).build();
    }
}