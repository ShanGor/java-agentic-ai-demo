package cn.gzten.mcp_server_demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FairyTaleServiceTest {

    @Test
    void testTellStory() {
        FairyTaleService fairyTaleService = new FairyTaleService();
        String fairyTale = fairyTaleService.tellStory("Anderson");
        System.out.println(fairyTale);
        assertNotNull(fairyTale);
    }
}