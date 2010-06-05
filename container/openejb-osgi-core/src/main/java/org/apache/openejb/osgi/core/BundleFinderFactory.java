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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.osgi.core;

import org.apache.openejb.config.DeploymentModule;
import org.apache.openejb.config.FinderFactory;
import org.apache.xbean.finder.AbstractFinder;
import org.apache.xbean.finder.BundleAnnotationFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @version $Rev$ $Date$
 */
public class BundleFinderFactory extends FinderFactory {

    @Override
    public AbstractFinder create(DeploymentModule module) throws Exception {
        ClassLoader cl = BundleFinderFactory.class.getClassLoader();
        ClassLoader moduleCL = module.getClassLoader();

        if (cl instanceof BundleReference && moduleCL instanceof BundleReference) {
            Bundle bundle = getBundle(cl);
            BundleContext bundleContext = bundle.getBundleContext();
            ServiceReference sr = bundleContext.getServiceReference(PackageAdmin.class.getName());
            PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(sr);

            return new BundleAnnotationFinder(packageAdmin, getBundle(moduleCL));
        }

        return super.create(module);
    }

    private Bundle getBundle(ClassLoader cl) {
        return ((BundleReference) cl).getBundle();
    }

}