package net.whydah.service.authorizations;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import junit.framework.TestCase;
import org.glassfish.jersey.server.mvc.Viewable;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAuthorizationResourceTest extends TestCase {


    private Configuration freemarkerConfig;

    @Test
    public void testTempleteOverride() {
        try {
            String templateName = "/UserAuthorization.ftl";

            Viewable userAuthorizationGui = new Viewable("/UserAuthorization.ftl", null);

            loadTemplates();
            Template template = freemarkerConfig.getTemplate(templateName);
            System.out.printf("template");
            //template.process(model, stringWriter);
            Viewable userAuthorizaionGui = new Viewable("/UserAuthorization.ftl", null);
            System.out.printf("template");
            Map<String, String> user = new HashMap<>();
            user.put("name", "username");
            user.put("id", "userid");
            Map<String, Object> model = new HashMap<>();
            model.put("user", user);

            model.put("client_id", "clientId");
            model.put("client_name", "clientName");
            model.put("scope", "scope");
            model.put("response_type", "response_type");
            model.put("state", "state");
            model.put("redirect_uri", "redirect_uri");
            model.put("customer_ref", "userToken.getPersonRef()");
            model.put("usertoken_id", "userTokenIdFromCookie");
            model.put("nonce", "nonce");

            List<String> scopes = Arrays.asList("sup1", "sup2", "sup3");
            ;
            model.put("scopeList", scopes);

            String body = createBody(templateName, model);
            System.out.println(body);

        } catch (Exception e) {
            System.out.println("error" + e);
        }
    }

    public String createBody(String templateName, Map<String, Object> model) {
        StringWriter stringWriter = new StringWriter();
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, stringWriter);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Populating template failed. templateName=" + templateName, e);
        }
        return stringWriter.toString();
    }

    private void loadTemplates() {
        try {
            freemarkerConfig = new Configuration(Configuration.VERSION_2_3_0);
            File customTemplate = new File("./templates");
            FileTemplateLoader ftl = null;
            if (customTemplate.exists()) {
                ftl = new FileTemplateLoader(customTemplate);
            }
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/templates");

            TemplateLoader[] loaders = null;
            if (ftl != null) {
                loaders = new TemplateLoader[]{ftl, ctl};
            } else {
                loaders = new TemplateLoader[]{ctl};
            }

            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
            freemarkerConfig.setTemplateLoader(mtl);
            freemarkerConfig.setObjectWrapper(new DefaultObjectWrapper());
            freemarkerConfig.setDefaultEncoding("UTF-8");
            freemarkerConfig.setLocalizedLookup(false);
            freemarkerConfig.setTemplateUpdateDelayMilliseconds(6000);
        } catch (IOException ioe) {
            //log.error("Unable to load/process freenmarker tenmplates",ioe);
        }
    }
}