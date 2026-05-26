/*
 * Copyright 2024 Arthas Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.arthas.core;

import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.LogUtil;
import org.slf4j.Logger;

import java.arthas.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Properties;

/**
 * Arthas core entry point.
 *
 * <p>This class serves as the main entry point for attaching Arthas to a running JVM.
 * It is invoked via the Java Attach API when the Arthas agent is attached to a target process.
 */
public class Arthas {

    private static final Logger logger = LogUtil.getArthasLogger();

    private Arthas() {
        // utility class, prevent instantiation
    }

    /**
     * Agent entry point invoked when Arthas is loaded as a Java agent at JVM startup.
     *
     * @param args            agent arguments passed via -javaagent flag
     * @param instrumentation the instrumentation instance provided by the JVM
     */
    public static void premain(String args, Instrumentation instrumentation) {
        main(args, instrumentation);
    }

    /**
     * Agent entry point invoked when Arthas is dynamically attached to a running JVM.
     *
     * @param args            agent arguments
     * @param instrumentation the instrumentation instance provided by the JVM
     */
    public static void agentmain(String args, Instrumentation instrumentation) {
        main(args, instrumentation);
    }

    /**
     * Common initialization logic shared by both premain and agentmain.
     *
     * @param args            agent arguments (key=value pairs separated by semicolons)
     * @param instrumentation the instrumentation instance
     */
    private static synchronized void main(String args, final Instrumentation instrumentation) {
        try {
            // Log the Java version alongside the args for easier debugging across environments
            logger.info("Arthas agent starting, args: {}, java.version: {}", args,
                    System.getProperty("java.version"));

            // Parse the agent arguments into a Configure object
            Configure configure = parseArguments(args);

            // Initialize the SpyAPI for bytecode instrumentation hooks
            SpyAPI.init();

            // Bootstrap the Arthas server
            ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(instrumentation, configure);
            bootstrap.bind(configure);

            logger.info("Arthas agent started successfully.");
        } catch (Throwable t) {
            logger.
