package org.apache.openejb.arquillian.session;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.openejb.util.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author rmannibucau
 */
@RunWith(Arquillian.class)
public class SessionScopeTest {
    public static final String TEST_SESSION_URL = "http://127.0.0.1:9080/test/session";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addClass(PojoSessionScoped.class).addClass(PojoSessionScopedServletWrapper.class)
            .addAsLibraries(new File("target/test-libs/commons-httpclient.jar"))
            .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class)
                .version("3.0")
                .servlet(PojoSessionScopedServletWrapper.class, "/session")
                .exportAsString()));
    }

    @Test
    public void testShouldBeAbleToAccessServletAndEjb() throws Exception {
        String[] sessionResult = new String[2];
        for (int i = 0; i < sessionResult.length; i++) {
            HttpClient client = new HttpClient();
            HttpMethod get = new GetMethod(TEST_SESSION_URL);
            String[] contents = new String[2];
            try {
                for (int j = 0; j < contents.length; j++) {
                    int out = client.executeMethod(get);
                    if (out != 200) {
                        throw new RuntimeException("get " + TEST_SESSION_URL + " returned " + out);
                    }
                    contents[j] = get.getResponseBodyAsString();
                }

                assertEquals(contents[0], contents[1]);
            } finally {
                get.releaseConnection();
            }
            sessionResult[i] = contents[0];
        }

        assertNotSame(sessionResult[0], sessionResult[1]);
    }
}
