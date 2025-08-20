package net.whydah.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class FreeMarkerHelper {
	
	private static final Logger log = getLogger(FreeMarkerHelper.class);
	
	private static Configuration freemarkerConfig;
	
	static {
		loadTemplates();
	}
	
	public static String createBody(String templateName, Map<String, Object> model) {
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
	
	private static void loadTemplates() {
		try {
			freemarkerConfig = new Configuration(Configuration.VERSION_2_3_0);
			File customTemplate = new File("./templates");
			FileTemplateLoader ftl = null;
			if (customTemplate.exists()) {
				ftl = new FileTemplateLoader(customTemplate);
			}
			ClassTemplateLoader ctl = new ClassTemplateLoader(FreeMarkerHelper.class, "/templates");

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
			log.error("Unable to load/process freemarker tenmplates", ioe);
		}
	}


}
