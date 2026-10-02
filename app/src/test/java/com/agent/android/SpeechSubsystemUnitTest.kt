package com.agent.android

import com.agent.android.speech.AgentLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SpeechSubsystemUnitTest {

    @Test
    fun testAgentLanguageEnum() {
        assertEquals("en", AgentLanguage.ENGLISH.code)
        assertEquals("te", AgentLanguage.TELUGU.code)
        assertEquals("hi", AgentLanguage.HINDI.code)
        assertEquals("kn", AgentLanguage.KANNADA.code)

        assertEquals("en-US", AgentLanguage.ENGLISH.sttTag)
        assertEquals("te-IN", AgentLanguage.TELUGU.sttTag)
        assertEquals("hi-IN", AgentLanguage.HINDI.sttTag)
        assertEquals("kn-IN", AgentLanguage.KANNADA.sttTag)

        assertEquals(AgentLanguage.TELUGU, AgentLanguage.fromCode("te"))
        assertEquals(AgentLanguage.HINDI, AgentLanguage.fromCode("hi"))
        assertEquals(AgentLanguage.KANNADA, AgentLanguage.fromCode("kn"))
        assertEquals(AgentLanguage.ENGLISH, AgentLanguage.fromCode("unknown"))
    }

    @Test
    fun testAgentLanguageLocales() {
        assertNotNull(AgentLanguage.ENGLISH.locale)
        assertNotNull(AgentLanguage.TELUGU.locale)
        assertNotNull(AgentLanguage.HINDI.locale)
        assertNotNull(AgentLanguage.KANNADA.locale)
    }
}
