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
            logger.info("Arthas agent starting, args: {}", args);

            // Parse the agent arguments into a Configure object
            Configure configure = parseArguments(args);

            // Initialize the SpyAPI for bytecode instrumentation hooks
            SpyAPI.init();

            // Bootstrap the Arthas server
            ArthasBootstrap bootstrap = ArthasBootstrap.getInstance(instrumentation, configure);
            bootstrap.bind(configure);

            logger.info("Arthas agent started successfully.");
        } catch (Throwable t) {
            logger.error("Arthas agent failed to start.", t);
            throw new RuntimeException("Arthas agent failed to start.", t);
        }
    }

    /**
     * Parses agent arguments from a semicolon-delimited key=value string.
     *
     * <p>Example input: {@code ip=127.0.0.1;port=3658;sessionTimeout=1800}
     *
     * @param args the raw argument string
     * @return a populated {@link Configure} instance
     */
    private static Configure parseArguments(String args) {
        Configure configure = new Configure();
        if (args == null || args.trim().isEmpty()) {
            return configure;
        }

        Properties props = new Properties();
        for (String pair : args.split(";")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx).trim();
                String value = pair.substring(idx + 1).trim();
                props.setProperty(key, value);
            }
        }

        // Apply known configuration properties
        if (props.containsKey("ip")) {
            configure.setIp(props.getProperty("ip"));
        }
        if (props.containsKey("port")) {
            configure.setTelnetPort(Integer.parseInt(props.getProperty("port")));
        }
        if (props.containsKey("httpPort")) {
            configure.setHttpPort(Integer.parseInt(props.getProperty("httpPort")));
        }
        if (props.containsKey("sessionTimeout")) {
            configure.setSessionTimeout(Integer.parseInt(props.getProperty("sessionTimeout")));
        }
        if (props.containsKey("targetIp")) {
            configure.setTargetIp(props.getProperty("targetIp"));
        }
        if (props.containsKey("tunnelServer")) {
            configure.setTunnelServer(props.getProperty("tunnelServer"));
        }
        if (props.containsKey("agentId")) {
            configure.setAgentId(props.getProperty("agentId"));
        }

        logger.debug("Parsed Arthas configuration: {}", configure);
        return configure;
    }
}
