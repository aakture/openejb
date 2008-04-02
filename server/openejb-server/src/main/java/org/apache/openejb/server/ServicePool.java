/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.server;

import static org.apache.openejb.server.ServiceDaemon.getInt;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

public class ServicePool implements ServerService {
    private static final Logger log = Logger.getInstance(LogCategory.SERVICEPOOL, "org.apache.openejb.util.resources");

    private final ServerService next;
    private final Executor executor;

    public ServicePool(ServerService next, final String name, Properties properties) {
        this.next = next;

        final int threads = getInt(properties, "threads", 100);

        final int keepAliveTime = (1000 * 60 * 5);

        ThreadPoolExecutor p = new ThreadPoolExecutor(threads, threads, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
        p.setThreadFactory(new ThreadFactory() {
            private volatile int id = 0;

            public Thread newThread(Runnable arg0) {
                Thread thread = new Thread(arg0, name + " " + getNextID());
                return thread;
            }

            private int getNextID() {
                return id++;
            }

        });
        executor = p;
    }

    public ServicePool(ServerService next, Executor executor) {
        this.next = next;
        this.executor = executor;
    }

    public void service(InputStream in, OutputStream out) throws ServiceException, IOException {
    }

    public void service(final Socket socket) throws ServiceException, IOException {
        final Runnable service = new Runnable() {
            public void run() {
                try {
                    next.service(socket);
                } catch (SecurityException e) {
                    log.error("Security error: " + e.getMessage(), e);
                } catch (Throwable e) {
                    log.error("Unexpected error", e);
                } finally {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (Throwable t) {
                        log.warning("Error while closing connection with client", t);
                    }
                }
            }
        };

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Runnable ctxCL = new Runnable() {
            public void run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(tccl);
                try {
                    service.run();
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
            }
        };

        executor.execute(ctxCL);
    }

    /**
     * Pulls out the access log information
     *
     * @param props
     * @throws ServiceException
     */
    public void init(Properties props) throws Exception {
        // Do our stuff

        // Then call the next guy
        next.init(props);
    }

    public void start() throws ServiceException {
        // Do our stuff

        // Then call the next guy
        next.start();
    }

    public void stop() throws ServiceException {
        // Do our stuff

        // Then call the next guy
        next.stop();
    }


    /**
     * Gets the name of the service.
     * Used for display purposes only
     */
    public String getName() {
        return next.getName();
    }

    /**
     * Gets the ip number that the
     * daemon is listening on.
     */
    public String getIP() {
        return next.getIP();
    }

    /**
     * Gets the port number that the
     * daemon is listening on.
     */
    public int getPort() {
        return next.getPort();
    }

}
