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
package org.apache.openejb.arquillian.embedded;

import org.apache.openejb.AppContext;
import org.apache.openejb.arquillian.common.FileUtils;
import org.apache.openejb.arquillian.common.TomEEConfiguration;
import org.apache.openejb.arquillian.common.TomEEContainer;
import org.apache.openejb.util.NetworkUtil;
import org.apache.tomee.embedded.Configuration;
import org.apache.tomee.embedded.Container;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import java.io.File;

public class EmbeddedTomEEContainer extends TomEEContainer {

    public static final String TOMEE_ARQUILLIAN_HTTP_PORT = "tomee.arquillian.http";
    public static final String TOMEE_ARQUILLIAN_STOP_PORT = "tomee.arquillian.stop";

    @Inject @ContainerScoped private InstanceProducer<Context> contextInstance;
    @Inject @DeploymentScoped private InstanceProducer<BeanManager> beanManagerInstance;

    private Container container;

    public EmbeddedTomEEContainer() {
        container = new Container();
    }

    public Class<TomEEConfiguration> getConfigurationClass() {
        return TomEEConfiguration.class;
    }

    public void setup(TomEEConfiguration configuration) {
        container.setup(convertConfiguration(configuration));
        this.configuration = configuration;
    }

    /*
     * Not exactly as elegant as I'd like. Maybe we could have the EmbeddedServer configuration in openejb-core so all the adapters can use it.
     * Depending on tomee-embedded is fine in this adapter, but less desirable in the others, as we'd get loads of stuff in the classpath we don't need.
     */
    private Configuration convertConfiguration(TomEEConfiguration tomeeConfiguration) {
    	Configuration configuration = new Configuration();
    	configuration.setDir(tomeeConfiguration.getDir());
    	configuration.setHttpPort(getPortAndShare(TOMEE_ARQUILLIAN_HTTP_PORT, tomeeConfiguration.getHttpPort()));
    	configuration.setStopPort(getPortAndShare(TOMEE_ARQUILLIAN_STOP_PORT, tomeeConfiguration.getStopPort()));
    	
		return configuration;
	}

    private int getPortAndShare(String systemPropName, int value) {
        int port = value;
        if (port <= 0) {
            port = NetworkUtil.getNextAvailablePort();
        }
        System.setProperty(systemPropName, Integer.toString(port));
        return port;
    }

    public void start() throws LifecycleException {
        try {
            container.start();
            contextInstance.set(container.getJndiContext());
        } catch (Exception e) {
            e.printStackTrace();
            throw new LifecycleException("Something went wrong", e);
        }
    }

    public void stop() throws LifecycleException {
        try {
            container.stop();
        } catch (Exception e) {
            throw new LifecycleException("Unable to stop server", e);
        }
    }

    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Local");
    }
    
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
    	try {

            final File tempDir = FileUtils.createTempDir();
            final String name = archive.getName();
            final File file = new File(tempDir, name);
        	archive.as(ZipExporter.class).exportTo(file, true);


            AppContext appContext = container.deploy(name, file);

            HTTPContext httpContext = new HTTPContext("0.0.0.0", configuration.getHttpPort());
            httpContext.add(new Servlet("ArquillianServletRunner", "/" + getArchiveNameWithoutExtension(archive)));
            beanManagerInstance.set(appContext.getBeanManager());
            return new ProtocolMetaData().addContext(httpContext);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeploymentException("Unable to deploy", e);
        }
    }

    public void undeploy(Archive<?> archive) throws DeploymentException {
    	try {
            final String name = archive.getName();
            container.undeploy(name);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeploymentException("Unable to undeploy", e);
        }
    }
}
