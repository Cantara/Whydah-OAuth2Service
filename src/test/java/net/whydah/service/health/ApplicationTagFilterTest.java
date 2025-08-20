package net.whydah.service.health;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Tag;

public class ApplicationTagFilterTest {
    private final static Logger log = LoggerFactory.getLogger(ApplicationTagFilterTest.class);


    @Test
    public void testSimpleTagMapping() {
        String simpletags = "HIDDEN, JURISDICTION_NORWAY";

        List<Tag> tagList = ApplicationTagMapper.getTagList(simpletags);
        assertTrue(tagList.size() == 2);

        log.debug(ApplicationTagMapper.toJson(tagList));
        log.debug(ApplicationTagMapper.toApplicationTagString(tagList));
    }


    @Test
    public void testFilterTagMapping() {
        String simpletags = "HIDDEN, JURISDICTION_NORWAY";

        List<Tag> tagList = ApplicationTagMapper.getTagList(simpletags);
        assertTrue(tagList.size() == 2);

        for (Tag tag : tagList) {
            if (tag.toString().contains("HIDDEN")) {
                log.debug("Filtering tag:" + tag);
            } else {
                log.debug("Accepting tag:" + tag);
            }
        }
    }
}
